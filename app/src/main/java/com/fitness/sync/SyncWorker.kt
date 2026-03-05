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
        Log.i(TAG, "🚀 [DEBUG] 开始同步任务。指定日期: ${specificDate ?: "ALL"}")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val googleSignInClient = GoogleSignIn.getClient(applicationContext, gso)
        
        val account = try {
            val task = googleSignInClient.silentSignIn()
            Tasks.await(task)
        } catch (e: Exception) {
            Log.e(TAG, "❌ [DEBUG] 静默登录失败", e)
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        }

        if (account == null) {
            Log.e(TAG, "❌ [DEBUG] 同步终止：未找到 Google 账户。")
            return@withContext Result.success()
        }

        Log.d(TAG, "✅ [DEBUG] 已获得账户: ${account.email}")

        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("FitBot").build()

        val helper = DriveServiceHelper(driveService)
        
        try {
            val folderId = helper.getOrCreateFolder("MyFitnessData")
            Log.i(TAG, "📁 [DEBUG] 云端文件夹确认成功，ID: $folderId")

            // 1. 同步记录
            syncSetsLogic(helper, folderId, specificDate)

            // 2. 同步计划
            syncPlansAtomicLogic(helper, folderId)

            // 3. 同步偏好
            syncPrefs(helper, folderId)

            Log.i(TAG, "🏁 [DEBUG] 全量同步流程结束。")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ [DEBUG] 同步过程发生严重错误", e)
            Result.retry()
        }
    }

    private suspend fun syncSetsLogic(helper: DriveServiceHelper, folderId: String, specificDate: String?) {
        val dbDates = exerciseDao.getDistinctDates()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val datesToSync = if (specificDate != null) listOf(specificDate) else {
            (dbDates.toSet() + today).toList()
        }
        
        Log.i(TAG, "📅 [DEBUG] 待同步日期列表: $datesToSync")
        datesToSync.forEach { date -> syncSets(helper, folderId, date) }
    }

    private suspend fun syncSets(helper: DriveServiceHelper, folderId: String, date: String) {
        val fileName = "$date.json"
        
        // 拉取云端
        val remoteJson = helper.downloadFile(folderId, fileName)
        if (remoteJson != null) {
            Log.d(TAG, "☁️ [DEBUG] 发现云端历史记录 $fileName, 长度: ${remoteJson.length}")
            try {
                val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                val remoteSets = transformToSetEntities(remoteDay)
                val existingIds = exerciseDao.getAllRemoteIds().toSet()
                remoteSets.forEach { if (!existingIds.contains(it.remoteId)) exerciseDao.insertSet(it) }
            } catch (e: Exception) { Log.e(TAG, "❌ [DEBUG] 解析云端 $fileName 失败", e) }
        }

        // 推送本地
        val allSets = exerciseDao.getSetsByDate(date)
        if (allSets.isNotEmpty()) {
            val trainingDay = transformToTrainingDay(date, allSets)
            val jsonContent = gson.toJson(trainingDay)
            Log.i(TAG, "📤 [DEBUG] 准备上传 $fileName, 内容: $jsonContent")
            try {
                helper.uploadOrUpdateFile(folderId, fileName, jsonContent)
                Log.i(TAG, "✅ [DEBUG] 上传 $fileName 成功")
            } catch (e: Exception) { Log.e(TAG, "❌ [DEBUG] 上传 $fileName 失败", e) }
        } else {
            Log.v(TAG, "⏭️ [DEBUG] 日期 $date 本地无数据，跳过上传。")
        }
    }

    private suspend fun syncPlansAtomicLogic(helper: DriveServiceHelper, folderId: String) {
        Log.i(TAG, "📋 [DEBUG] 开始同步原子化计划...")
        val allLocalPlans = planDao.getAllPlans()
        Log.d(TAG, "📊 [DEBUG] 本地计划总数: ${allLocalPlans.size}")

        allLocalPlans.forEach { localPlan ->
            val fileName = "plan_history_${localPlan.createdAt}.json"
            val jsonContent = gson.toJson(localPlan)
            Log.v(TAG, "📤 [DEBUG] 检查归档计划: $fileName")
            try {
                helper.uploadOrUpdateFile(folderId, fileName, jsonContent)
            } catch (e: Exception) { Log.e(TAG, "❌ [DEBUG] 上传计划归档 $fileName 失败", e) }
        }

        val currentPlans = allLocalPlans.filter { it.isCurrent }
        val plansJson = gson.toJson(currentPlans)
        Log.i(TAG, "📤 [DEBUG] 准备上传主计划 plans.json, 内容: $plansJson")
        helper.uploadOrUpdateFile(folderId, "plans.json", plansJson)
    }

    private suspend fun syncPrefs(helper: DriveServiceHelper, folderId: String) {
        val prefs = applicationContext.dataStore.data.first()
        val localPrefsMap = mapOf(
            "theme_mode" to (prefs[stringPreferencesKey("theme_mode")] ?: "system"),
            "language" to (prefs[stringPreferencesKey("language")] ?: "zh"),
            "user_quote" to (prefs[stringPreferencesKey("user_quote")] ?: "坚持就是胜利")
        )
        val jsonContent = gson.toJson(localPrefsMap)
        Log.i(TAG, "📤 [DEBUG] 同步偏好设置: $jsonContent")
        helper.uploadOrUpdateFile(folderId, "user_prefs.json", jsonContent)
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
