package com.fitness.sync

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitness.data.local.AppDatabase
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val specificDate = inputData.getString("SYNC_DATE")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val googleSignInClient = GoogleSignIn.getClient(applicationContext, gso)
        
        val account = try {
            val task = googleSignInClient.silentSignIn()
            if (task.isSuccessful) task.result else GoogleSignIn.getLastSignedInAccount(applicationContext)
        } catch (e: Exception) {
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        }

        if (account == null) return@withContext Result.success()

        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("FitBot").build()

        val helper = DriveServiceHelper(driveService)
        
        try {
            val folderId = helper.getOrCreateFolder("MyFitnessData")

            // 1. 同步锻炼记录
            syncSetsLogic(helper, folderId, specificDate)

            // 2. 同步训练计划 (带归档逻辑)
            syncPlansLogic(helper, folderId)

            // 3. 同步偏好设置
            syncPrefs(helper, folderId)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync process failed", e)
            Result.retry()
        }
    }

    private suspend fun syncSetsLogic(helper: DriveServiceHelper, folderId: String, specificDate: String?) {
        val datesToSync = if (specificDate != null) listOf(specificDate) else {
            val dbDates = exerciseDao.getDistinctDates().toMutableSet()
            dbDates.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
            dbDates.toList()
        }
        datesToSync.forEach { date -> syncSets(helper, folderId, date) }
    }

    private suspend fun syncSets(helper: DriveServiceHelper, folderId: String, date: String) {
        val fileName = "$date.json"
        val remoteJson = helper.downloadFile(folderId, fileName)
        val existingRemoteIds = exerciseDao.getAllRemoteIds().toSet()

        if (remoteJson != null) {
            try {
                val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                val remoteSets = transformToSetEntities(remoteDay)
                remoteSets.forEach { if (!existingRemoteIds.contains(it.remoteId)) exerciseDao.insertSet(it) }
            } catch (e: Exception) { Log.e(TAG, "Error parsing $fileName", e) }
        }

        val allSets = exerciseDao.getSetsByDate(date)
        if (allSets.isNotEmpty()) {
            val trainingDay = transformToTrainingDay(date, allSets)
            helper.uploadOrUpdateFile(folderId, fileName, gson.toJson(trainingDay))
        }
    }

    /**
     * 实现计划拆分同步：plans.json (当前) vs archived_plans.json (历史)
     */
    private suspend fun syncPlansLogic(helper: DriveServiceHelper, folderId: String) {
        // --- 1. 处理 plans.json (仅当前生效计划) ---
        val remotePlansJson = helper.downloadFile(folderId, "plans.json")
        if (remotePlansJson != null) {
            try {
                val type = object : TypeToken<List<PlanEntity>>() {}.type
                val remotePlans: List<PlanEntity> = gson.fromJson(remotePlansJson, type) ?: emptyList()
                remotePlans.forEach { planDao.insertPlan(it) }
            } catch (e: Exception) { Log.e(TAG, "Error parsing plans.json", e) }
        }

        // --- 2. 处理 archived_plans.json (历史归档) ---
        val remoteArchivedJson = helper.downloadFile(folderId, "archived_plans.json")
        if (remoteArchivedJson != null) {
            try {
                val type = object : TypeToken<List<PlanEntity>>() {}.type
                val remoteArchived: List<PlanEntity> = gson.fromJson(remoteArchivedJson, type) ?: emptyList()
                remoteArchived.forEach { planDao.insertPlan(it) }
            } catch (e: Exception) { Log.e(TAG, "Error parsing archived_plans.json", e) }
        }

        // --- 3. 重新导出并上传 ---
        val allLocalPlans = planDao.getAllPlans()
        val currentPlans = allLocalPlans.filter { it.isCurrent }
        val archivedPlans = allLocalPlans.filter { !it.isCurrent }

        // 上传当前计划
        helper.uploadOrUpdateFile(folderId, "plans.json", gson.toJson(currentPlans))
        
        // 如果有归档，上传归档文件
        if (archivedPlans.isNotEmpty()) {
            helper.uploadOrUpdateFile(folderId, "archived_plans.json", gson.toJson(archivedPlans))
        }
    }

    private suspend fun syncPrefs(helper: DriveServiceHelper, folderId: String) {
        val prefs = applicationContext.dataStore.data.first()
        val localPrefsMap = mapOf(
            "theme_mode" to (prefs[stringPreferencesKey("theme_mode")] ?: "system"),
            "language" to (prefs[stringPreferencesKey("language")] ?: "zh"),
            "user_quote" to (prefs[stringPreferencesKey("user_quote")] ?: "坚持就是胜利")
        )
        helper.uploadOrUpdateFile(folderId, "user_prefs.json", gson.toJson(localPrefsMap))
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
