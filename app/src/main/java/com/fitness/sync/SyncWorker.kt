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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val specificDate = inputData.getString("SYNC_DATE")
        Log.i(TAG, "Starting sync worker. Specific date: ${specificDate ?: "ALL"}")
        
        // 1. 获取 Google 账户并确保权限
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE)) // 统一使用 DRIVE_FILE 权限
            .build()
        val googleSignInClient = GoogleSignIn.getClient(applicationContext, gso)
        
        val account = try {
            val task = googleSignInClient.silentSignIn()
            Tasks.await(task) // 同步等待授权刷新
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in failed", e)
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        }

        if (account == null) {
            Log.e(TAG, "No Google account found. Terminating.")
            return@withContext Result.success()
        }

        // 2. 初始化 Drive 服务
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("FitBot").build()

        val helper = DriveServiceHelper(driveService)
        
        try {
            // 获取文件夹 ID
            val folderId = helper.getOrCreateFolder("MyFitnessData")

            // 3. 同步锻炼记录
            val dbDates = exerciseDao.getDistinctDates()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val datesToSync = if (specificDate != null) listOf(specificDate) else (dbDates.toSet() + today).toList()
            
            datesToSync.forEach { date ->
                syncSets(helper, folderId, date)
            }

            // 4. 同板同步：原子化归档计划
            syncPlansAtomic(helper, folderId)

            // 5. 同步偏好设置
            syncPrefs(helper, folderId)

            Log.i(TAG, "Sync successful.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            Result.retry()
        }
    }

    private suspend fun syncSets(helper: DriveServiceHelper, folderId: String, date: String) {
        val fileName = "$date.json"
        val remoteJson = helper.downloadFile(folderId, fileName)
        val existingRemoteIds = exerciseDao.getAllRemoteIds().toSet()

        // 合并云端到本地
        if (remoteJson != null) {
            try {
                val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                val remoteSets = transformToSetEntities(remoteDay)
                remoteSets.forEach { if (!existingRemoteIds.contains(it.remoteId)) exerciseDao.insertSet(it) }
            } catch (e: Exception) { Log.e(TAG, "Parse error for $date", e) }
        }

        // 推送本地到云端
        val localSets = exerciseDao.getSetsByDate(date)
        if (localSets.isNotEmpty()) {
            val json = gson.toJson(transformToTrainingDay(date, localSets))
            helper.uploadOrUpdateFile(folderId, fileName, json)
        }
    }

    private suspend fun syncPlansAtomic(helper: DriveServiceHelper, folderId: String) {
        val remoteHistoryFiles = helper.queryFiles(folderId, "name contains 'plan_history_'")
        val localPlans = planDao.getAllPlans().associateBy { it.createdAt }

        // 下载缺失的历史计划
        remoteHistoryFiles.forEach { remoteFile ->
            val timestamp = remoteFile.name.substringAfter("plan_history_").substringBefore(".json").toLongOrNull()
            if (timestamp != null && !localPlans.containsKey(timestamp)) {
                try {
                    val content = helper.downloadFileById(remoteFile.id)
                    val remotePlan = gson.fromJson(content, PlanEntity::class.java)
                    // Room 会根据 Index 自动处理或根据 Insert Strategy 覆盖
                    planDao.insertPlan(remotePlan.copy(id = 0)) 
                } catch (e: Exception) { Log.e(TAG, "Error syncing plan ${remoteFile.name}", e) }
            }
        }

        // 上传本地新生成的归档
        planDao.getAllPlans().forEach { localPlan ->
            val fileName = "plan_history_${localPlan.createdAt}.json"
            if (remoteHistoryFiles.none { it.name == fileName }) {
                helper.uploadOrUpdateFile(folderId, fileName, gson.toJson(localPlan))
            }
        }

        // 刷新 plans.json (仅含当前活跃)
        val current = planDao.getCurrentPlan()
        if (current != null) {
            helper.uploadOrUpdateFile(folderId, "plans.json", gson.toJson(listOf(current)))
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
