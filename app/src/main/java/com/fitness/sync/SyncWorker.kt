package com.fitness.sync

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitness.data.local.ExerciseDao
import com.fitness.data.local.PlanDao
import com.fitness.data.local.PlanEntity
import com.fitness.data.local.SetEntity
import com.fitness.model.*
import com.fitness.ui.profile.dataStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val exerciseDao: ExerciseDao,
    private val planDao: PlanDao
) : CoroutineWorker(appContext, workerParams) {

    private val gson = Gson()
    private val TAG = "FitBotSync"
    private val FOLDER_NAME = "FitBot"
    private val LAST_SYNC_KEY = longPreferencesKey("last_sync_time")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting optimized sync worker...")
        
        val account = try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
            val task = GoogleSignIn.getClient(applicationContext, gso).silentSignIn()
            Tasks.await(task)
        } catch (e: Exception) {
            Log.e(TAG, "Auth: Silent sign-in failed - ${e.message}")
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        }

        if (account == null) {
            Log.e(TAG, "Auth: No account found. Sync aborted.")
            return@withContext Result.success()
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("FitBot").build()

        val helper = DriveServiceHelper(driveService)
        
        try {
            val folderId = helper.getOrCreateFolder(FOLDER_NAME)
            val remoteFiles = helper.queryFiles(folderId, "")
            val remoteFilesMap = remoteFiles.associateBy { it.name }
            
            val lastSyncTime = applicationContext.dataStore.data.first()[LAST_SYNC_KEY] ?: 0L

            syncSetsLogic(helper, folderId, remoteFilesMap, lastSyncTime)
            syncPlansLogic(helper, folderId, remoteFilesMap, lastSyncTime)
            syncPrefs(helper, folderId, remoteFilesMap, lastSyncTime)

            applicationContext.dataStore.edit { it[LAST_SYNC_KEY] = System.currentTimeMillis() }
            Log.i(TAG, "Sync complete.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync: Critical error - ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncSetsLogic(helper: DriveServiceHelper, folderId: String, remoteMap: Map<String, com.google.api.services.drive.model.File>, lastSync: Long) {
        val dbDates = exerciseDao.getDistinctDates().toMutableSet()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        dbDates.add(today)
        
        // 合并远程存在的日期
        remoteMap.keys.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.json")) }.forEach { 
            dbDates.add(it.substringBefore(".json"))
        }

        val existingIds = exerciseDao.getAllRemoteIds().toSet()

        dbDates.forEach { date ->
            val fileName = "$date.json"
            val remoteFile = remoteMap[fileName]
            var remoteJson: String? = null

            // 仅当远程有更新或本地缺失该日期数据时下载
            val shouldDownload = remoteFile != null && (remoteFile.modifiedTime.value > lastSync || !exerciseDao.getDistinctDates().contains(date))

            if (shouldDownload && remoteFile != null) {
                remoteJson = helper.downloadFileById(remoteFile.id)
                try {
                    val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                    transformToSetEntities(remoteDay).forEach { 
                        if (!existingIds.contains(it.remoteId)) exerciseDao.insertSet(it) 
                    }
                } catch (e: Exception) { Log.e(TAG, "Parse error for $date") }
            }

            val localSets = exerciseDao.getSetsByDate(date)
            if (localSets.isNotEmpty()) {
                val currentLocalDay = transformToTrainingDay(date, localSets)
                val json = gson.toJson(currentLocalDay)
                
                // 如果没有下载 remoteJson (因为没变)，但本地有数据，我们需要确保远程存在且一致
                if (json != remoteJson) {
                    if (remoteFile != null) {
                        helper.updateFile(remoteFile.id, json)
                    } else {
                        helper.createFile(folderId, fileName, json)
                    }
                }
            }
        }
    }

    private suspend fun syncPlansLogic(helper: DriveServiceHelper, folderId: String, remoteMap: Map<String, com.google.api.services.drive.model.File>, lastSync: Long) {
        // 1. 同步历史计划 (Append-only)
        val localPlans = planDao.getAllPlans().associateBy { it.createdAt }
        remoteMap.values.filter { it.name.startsWith("plan_history_") }.forEach { file ->
            val ts = file.name.substringAfter("plan_history_").substringBefore(".json").toLongOrNull()
            if (ts != null && !localPlans.containsKey(ts)) {
                try {
                    val content = helper.downloadFileById(file.id)
                    planDao.insertPlan(gson.fromJson(content, PlanEntity::class.java).copy(id = 0))
                } catch (e: Exception) { Log.e(TAG, "Plan pull error: ${file.name}") }
            }
        }
        
        planDao.getAllPlans().forEach { plan ->
            val name = "plan_history_${plan.createdAt}.json"
            if (!remoteMap.containsKey(name)) {
                helper.createFile(folderId, name, gson.toJson(plan))
            }
        }

        // 2. 同步当前计划 (Bidirectional)
        val currentPlanFile = remoteMap["plans.json"]
        if (currentPlanFile != null && currentPlanFile.modifiedTime.value > lastSync) {
            val content = helper.downloadFileById(currentPlanFile.id)
            val remotePlans = gson.fromJson<List<PlanEntity>>(content, object : TypeToken<List<PlanEntity>>() {}.type)
            remotePlans.firstOrNull()?.let { planDao.insertPlan(it.copy(id = 0)) }
        } else {
            planDao.getCurrentPlan()?.let { 
                val json = gson.toJson(listOf(it))
                if (currentPlanFile != null) helper.updateFile(currentPlanFile.id, json)
                else helper.createFile(folderId, "plans.json", json)
            }
        }
    }

    private suspend fun syncPrefs(helper: DriveServiceHelper, folderId: String, remoteMap: Map<String, com.google.api.services.drive.model.File>, lastSync: Long) {
        val prefFile = remoteMap["user_prefs.json"]
        if (prefFile != null && prefFile.modifiedTime.value > lastSync) {
            val remoteJson = helper.downloadFileById(prefFile.id)
            val remotePrefs = gson.fromJson<Map<String, String>>(remoteJson, object : TypeToken<Map<String, String>>() {}.type)
            applicationContext.dataStore.edit {
                remotePrefs["theme_mode"]?.let { v -> it[stringPreferencesKey("theme_mode")] = v }
                remotePrefs["language"]?.let { v -> it[stringPreferencesKey("language")] = v }
                remotePrefs["user_quote"]?.let { v -> it[stringPreferencesKey("user_quote")] = v }
            }
        } else {
            val prefs = applicationContext.dataStore.data.first()
            val map = mapOf(
                "theme_mode" to (prefs[stringPreferencesKey("theme_mode")] ?: "system"),
                "language" to (prefs[stringPreferencesKey("language")] ?: "zh"),
                "user_quote" to (prefs[stringPreferencesKey("user_quote")] ?: "坚持就是胜利")
            )
            val json = gson.toJson(map)
            if (prefFile != null) helper.updateFile(prefFile.id, json)
            else helper.createFile(folderId, "user_prefs.json", json)
        }
    }

    private fun transformToSetEntities(day: TrainingDay): List<SetEntity> {
        val result = mutableListOf<SetEntity>()
        day.sessions.forEach { session ->
            session.exercises.forEach { exercise ->
                exercise.sets.forEach { setRecord ->
                    val time = try {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("${day.date} ${setRecord.time}")?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) { System.currentTimeMillis() }
                    result.add(SetEntity(date = day.date, sessionId = session.sessionId, exerciseName = exercise.name, reps = setRecord.reps, weight = setRecord.weight, timestamp = time, timeStr = setRecord.time, remoteId = setRecord.remoteId))
                }
            }
        }
        return result
    }

    private fun transformToTrainingDay(date: String, sets: List<SetEntity>): TrainingDay {
        val sessions = sets.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
            val exercises = sessionSets.groupBy { it.exerciseName }.map { (exerciseName, exerciseSets) ->
                val setRecords = exerciseSets.map { SetRecord(reps = it.reps, weight = it.weight, time = it.timeStr, remoteId = it.remoteId) }
                ExerciseRecord(name = exerciseName, sets = setRecords)
            }
            TrainingSession(sessionId = sessionId, startTime = sessionSets.firstOrNull()?.timeStr ?: "00:00", endTime = sessionSets.lastOrNull()?.timeStr ?: "23:59", exercises = exercises)
        }
        return TrainingDay(date = date, sessions = sessions)
    }
}
