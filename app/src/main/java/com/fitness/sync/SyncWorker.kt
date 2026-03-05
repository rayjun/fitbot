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
    private val FOLDER_NAME = "FitBot" // 按照要求更新文件夹名称

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val specificDate = inputData.getString("SYNC_DATE")
        Log.i(TAG, "Starting sync worker. Target: ${specificDate ?: "FULL"}")
        
        // 1. 授权准备
        val account = try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
            Tasks.await(GoogleSignIn.getClient(applicationContext, gso).silentSignIn())
        } catch (e: Exception) {
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        }

        if (account == null) return@withContext Result.success()

        val credential = GoogleAccountCredential.usingOAuth2(applicationContext, listOf(DriveScopes.DRIVE_FILE))
            .apply { selectedAccount = account.account }

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("FitBot").build()

        val helper = DriveServiceHelper(driveService)
        
        try {
            // 2. 检查并创建 FitBot 文件夹
            val folderId = helper.getOrCreateFolder(FOLDER_NAME)

            // 3. 同步锻炼记录逻辑：对比并合并
            syncSetsWorkflow(helper, folderId, specificDate)

            // 4. 同步训练计划逻辑：基于版本号比对
            syncPlansWorkflow(helper, folderId)

            // 5. 同步偏好设置
            syncPrefs(helper, folderId)

            Log.i(TAG, "Sync logic alignment completed.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    /**
     * 锻炼记录同步流程：检查云端 -> 补全本地 -> 推送最新
     */
    private suspend fun syncSetsWorkflow(helper: DriveServiceHelper, folderId: String, specificDate: String?) {
        val dbDates = exerciseDao.getDistinctDates().toMutableSet()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val datesToSync = if (specificDate != null) listOf(specificDate) else (dbDates + today).toList()

        datesToSync.forEach { date ->
            val fileName = "$date.json"
            
            // A. 检查云端数据
            val remoteJson = helper.downloadFile(folderId, fileName)
            val localSets = exerciseDao.getSetsByDate(date)
            val existingRemoteIds = exerciseDao.getAllRemoteIds().toSet()

            if (remoteJson != null) {
                // 如果云端有数据，先补全本地没有的
                try {
                    val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                    val remoteSets = transformToSetEntities(remoteDay)
                    var merged = 0
                    remoteSets.forEach { 
                        if (!existingRemoteIds.contains(it.remoteId)) {
                            exerciseDao.insertSet(it)
                            merged++
                        }
                    }
                    if (merged > 0) Log.d(TAG, "Pulled $merged new sets from cloud for $date")
                } catch (e: Exception) { Log.e(TAG, "Remote data parse error for $date") }
            }

            // B. 获取合并后的最新本地数据
            val updatedLocalSets = exerciseDao.getSetsByDate(date)
            
            // C. 如果本地数据比云端新（数量多或云端没有），则推送到云端
            if (updatedLocalSets.isNotEmpty()) {
                val localJson = gson.toJson(transformToTrainingDay(date, updatedLocalSets))
                // 只有当内容不一致时才上传（uploadOrUpdateFile 内部有基础判断）
                if (localJson != remoteJson) {
                    helper.uploadOrUpdateFile(folderId, fileName, localJson)
                    Log.d(TAG, "Pushed latest local sets to cloud for $date")
                }
            }
        }
    }

    /**
     * 训练计划同步流程：比较版本号
     */
    private suspend fun syncPlansWorkflow(helper: DriveServiceHelper, folderId: String) {
        // 1. 处理所有原子化的历史计划文件
        val remoteHistoryFiles = helper.queryFiles(folderId, "name contains 'plan_history_'")
        val localPlans = planDao.getAllPlans().associateBy { it.createdAt }

        remoteHistoryFiles.forEach { remoteFile ->
            val timestamp = remoteFile.name.substringAfter("plan_history_").substringBefore(".json").toLongOrNull()
            if (timestamp != null && !localPlans.containsKey(timestamp)) {
                // 本地没有，从云端拉取
                val content = helper.downloadFileById(remoteFile.id)
                val remotePlan = gson.fromJson(content, PlanEntity::class.java)
                planDao.insertPlan(remotePlan.copy(id = 0))
                Log.d(TAG, "Pulled historical plan from cloud: ${remoteFile.name}")
            }
        }

        // 2. 检查本地是否有比云端更新的归档
        planDao.getAllPlans().forEach { localPlan ->
            val fileName = "plan_history_${localPlan.createdAt}.json"
            if (remoteHistoryFiles.none { it.name == fileName }) {
                helper.uploadOrUpdateFile(folderId, fileName, gson.toJson(localPlan))
                Log.d(TAG, "Pushed new local plan archive to cloud: $fileName")
            }
        }

        // 3. 刷新 plans.json (当前活跃计划索引)
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
