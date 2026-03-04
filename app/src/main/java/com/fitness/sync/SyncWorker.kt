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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val specificDate = inputData.getString("SYNC_DATE")
        Log.d(TAG, "Starting sync worker. Date override: $specificDate")
        
        // 1. 获取 Google 账户并强制刷新 Token
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val googleSignInClient = GoogleSignIn.getClient(applicationContext, gso)
        
        val account = try {
            // 使用 Tasks.await 同步等待静默登录结果
            val task = googleSignInClient.silentSignIn()
            Tasks.await(task)
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in failed, trying fallback to last account", e)
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        }

        if (account == null) {
            Log.e(TAG, "Sync failed: No Google account available.")
            return@withContext Result.success()
        }

        // 检查权限
        if (!GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            Log.e(TAG, "Sync failed: Missing DRIVE_FILE permission.")
            return@withContext Result.success()
        }

        // 2. 初始化 Drive 服务
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        val driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("FitBot").build()

        val helper = DriveServiceHelper(driveService)
        
        try {
            Log.d(TAG, "Initializing Google Drive folder...")
            val folderId = helper.getOrCreateFolder("MyFitnessData")
            Log.d(TAG, "Using Folder ID: $folderId")

            // 3. 同步锻炼记录
            syncSetsLogic(helper, folderId, specificDate)

            // 4. 同步训练计划 (原子化归档逻辑)
            syncPlansAtomicLogic(helper, folderId)

            // 5. 同步偏好设置
            syncPrefs(helper, folderId)

            Log.i(TAG, "Sync process finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Critical sync error", e)
            Result.retry()
        }
    }

    private suspend fun syncSetsLogic(helper: DriveServiceHelper, folderId: String, specificDate: String?) {
        val datesToSync = if (specificDate != null) {
            listOf(specificDate)
        } else {
            val dbDates = exerciseDao.getDistinctDates().toMutableSet()
            dbDates.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
            dbDates.toList()
        }
        
        Log.d(TAG, "Found ${datesToSync.size} dates to sync.")
        datesToSync.forEach { date ->
            try {
                syncSets(helper, folderId, date)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync date: $date", e)
            }
        }
    }

    private suspend fun syncSets(helper: DriveServiceHelper, folderId: String, date: String) {
        val fileName = "$date.json"
        Log.v(TAG, "Syncing sets for $date")
        
        val remoteJson = helper.downloadFile(folderId, fileName)
        val existingRemoteIds = exerciseDao.getAllRemoteIds().toSet()

        if (remoteJson != null) {
            try {
                val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                val remoteSets = transformToSetEntities(remoteDay)
                var newCount = 0
                remoteSets.forEach {
                    if (!existingRemoteIds.contains(it.remoteId)) {
                        exerciseDao.insertSet(it)
                        newCount++
                    }
                }
                if (newCount > 0) Log.d(TAG, "Merged $newCount new sets from remote for $date")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse remote data for $date", e)
            }
        }

        val allSets = exerciseDao.getSetsByDate(date)
        if (allSets.isNotEmpty()) {
            val trainingDay = transformToTrainingDay(date, allSets)
            helper.uploadOrUpdateFile(folderId, fileName, gson.toJson(trainingDay))
            Log.d(TAG, "Uploaded ${allSets.size} sets for $date")
        }
    }

    private suspend fun syncPlansAtomicLogic(helper: DriveServiceHelper, folderId: String) {
        Log.d(TAG, "Syncing atomic plans...")
        val remoteHistoryFiles = helper.queryFiles(folderId, "name contains 'plan_history_' and name contains '.json'")
        val localPlans = planDao.getAllPlans().associateBy { it.createdAt }

        remoteHistoryFiles.forEach { remoteFile ->
            val timestampStr = remoteFile.name.substringAfter("plan_history_").substringBefore(".json")
            val timestamp = timestampStr.toLongOrNull()
            
            if (timestamp != null && !localPlans.containsKey(timestamp)) {
                try {
                    val content = helper.downloadFileById(remoteFile.id)
                    val remotePlan = gson.fromJson(content, PlanEntity::class.java)
                    planDao.insertPlan(remotePlan.copy(id = 0))
                    Log.d(TAG, "Synced remote history plan: ${remoteFile.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading plan ${remoteFile.name}", e)
                }
            }
        }

        val allLocalPlans = planDao.getAllPlans()
        allLocalPlans.forEach { localPlan ->
            val fileName = "plan_history_${localPlan.createdAt}.json"
            val alreadyOnCloud = remoteHistoryFiles.any { it.name == fileName }
            if (!alreadyOnCloud) {
                helper.uploadOrUpdateFile(folderId, fileName, gson.toJson(localPlan))
                Log.d(TAG, "Uploaded local plan archive: $fileName")
            }
        }

        val currentPlans = allLocalPlans.filter { it.isCurrent }
        helper.uploadOrUpdateFile(folderId, "plans.json", gson.toJson(currentPlans))
    }

    private suspend fun syncPrefs(helper: DriveServiceHelper, folderId: String) {
        val prefs = applicationContext.dataStore.data.first()
        val localPrefsMap = mapOf(
            "theme_mode" to (prefs[stringPreferencesKey("theme_mode")] ?: "system"),
            "language" to (prefs[stringPreferencesKey("language")] ?: "zh"),
            "user_quote" to (prefs[stringPreferencesKey("user_quote")] ?: "坚持就是胜利")
        )
        helper.uploadOrUpdateFile(folderId, "user_prefs.json", gson.toJson(localPrefsMap))
        Log.d(TAG, "User preferences synced.")
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
