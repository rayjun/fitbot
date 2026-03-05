package com.fitness.sync

import android.content.Context
import android.util.Log
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting sync worker process...")
        
        // 1. 获取 Google 账户并强制同步 Token
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

        Log.d(TAG, "Auth: Success for account ${account.email}")

        // 2. 初始化 Drive 服务
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("FitBot").build()

        val helper = DriveServiceHelper(driveService)
        
        try {
            // 3. 检查/创建文件夹 (核心步骤)
            Log.d(TAG, "Drive: Searching for folder '$FOLDER_NAME'...")
            val folderId = helper.getOrCreateFolder(FOLDER_NAME)
            Log.i(TAG, "Drive: Folder identified successfully (ID: $folderId)")

            // 4. 同步逻辑
            syncSetsLogic(helper, folderId)
            syncPlansLogic(helper, folderId)
            syncPrefs(helper, folderId)

            Log.i(TAG, "Sync complete.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync: Critical error during execution - ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncSetsLogic(helper: DriveServiceHelper, folderId: String) {
        val dbDates = exerciseDao.getDistinctDates().toMutableSet()
        dbDates.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        
        dbDates.forEach { date ->
            val fileName = "$date.json"
            val remoteJson = helper.downloadFile(folderId, fileName)
            val existingIds = exerciseDao.getAllRemoteIds().toSet()

            if (remoteJson != null) {
                try {
                    val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                    transformToSetEntities(remoteDay).forEach { 
                        if (!existingIds.contains(it.remoteId)) exerciseDao.insertSet(it) 
                    }
                } catch (e: Exception) { Log.e(TAG, "Parse error for $date") }
            }

            val localSets = exerciseDao.getSetsByDate(date)
            if (localSets.isNotEmpty()) {
                val json = gson.toJson(transformToTrainingDay(date, localSets))
                if (json != remoteJson) {
                    helper.uploadOrUpdateFile(folderId, fileName, json)
                    Log.d(TAG, "Uploaded sets for $date")
                }
            }
        }
    }

    private suspend fun syncPlansLogic(helper: DriveServiceHelper, folderId: String) {
        val remoteFiles = helper.queryFiles(folderId, "name contains 'plan_history_'")
        val localPlans = planDao.getAllPlans().associateBy { it.createdAt }

        remoteFiles.forEach { file ->
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
            if (remoteFiles.none { it.name == name }) {
                helper.uploadOrUpdateFile(folderId, name, gson.toJson(plan))
            }
        }

        planDao.getCurrentPlan()?.let { 
            helper.uploadOrUpdateFile(folderId, "plans.json", gson.toJson(listOf(it))) 
        }
    }

    private suspend fun syncPrefs(helper: DriveServiceHelper, folderId: String) {
        val prefs = applicationContext.dataStore.data.first()
        val map = mapOf(
            "theme_mode" to (prefs[stringPreferencesKey("theme_mode")] ?: "system"),
            "language" to (prefs[stringPreferencesKey("language")] ?: "zh"),
            "user_quote" to (prefs[stringPreferencesKey("user_quote")] ?: "坚持就是胜利")
        )
        helper.uploadOrUpdateFile(folderId, "user_prefs.json", gson.toJson(map))
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
