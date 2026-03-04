package com.fitness.sync

import android.content.Context
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val specificDate = inputData.getString("SYNC_DATE")
        
        // 1. 获取已登录的 Google 账户
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) {
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
            val folderId = helper.getOrCreateFolder("MyFitnessData")

            // --- SYNC SETS (Enhanced) ---
            if (specificDate != null) {
                // 如果指定了日期（如刚录完动作），只同步那一天
                syncSets(helper, folderId, specificDate)
            } else {
                // 如果没指定日期（如手动点击“立即同步”），扫描所有有记录的日期
                val allDates = exerciseDao.getDistinctDates().toMutableSet()
                // 同时也要考虑今天（即使还没录入动作）
                allDates.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                
                allDates.forEach { date ->
                    syncSets(helper, folderId, date)
                }
            }

            // --- SYNC PLANS ---
            syncPlans(helper, folderId)

            // --- SYNC PREFS ---
            syncPrefs(helper, folderId)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun syncSets(helper: DriveServiceHelper, folderId: String, date: String) {
        // 先拉取云端记录
        val remoteJson = helper.downloadFile(folderId, "$date.json")
        val existingRemoteIds = exerciseDao.getAllRemoteIds().toSet()

        if (remoteJson != null) {
            try {
                val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                val remoteSets = transformToSetEntities(remoteDay)
                remoteSets.forEach { remoteSet ->
                    if (!existingRemoteIds.contains(remoteSet.remoteId)) {
                        exerciseDao.insertSet(remoteSet)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 后推送本地记录（包含合并后的）
        val allSets = exerciseDao.getSetsByDate(date)
        if (allSets.isNotEmpty()) {
            val trainingDay = transformToTrainingDay(date, allSets)
            val jsonString = gson.toJson(trainingDay)
            helper.uploadOrUpdateFile(folderId, "$date.json", jsonString)
        }
    }

    private suspend fun syncPlans(helper: DriveServiceHelper, folderId: String) {
        val remoteJson = helper.downloadFile(folderId, "plans.json")
        
        if (remoteJson != null) {
            try {
                val type = object : TypeToken<List<PlanEntity>>() {}.type
                val remotePlans: List<PlanEntity> = gson.fromJson(remoteJson, type) ?: emptyList()
                val localPlans = planDao.getAllPlans().associateBy { it.id }
                
                remotePlans.forEach { remotePlan ->
                    val localPlan = localPlans[remotePlan.id]
                    if (localPlan == null || remotePlan.version > localPlan.version) {
                        planDao.insertPlan(remotePlan)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val updatedLocalPlans = planDao.getAllPlans()
        if (updatedLocalPlans.isNotEmpty()) {
            val jsonString = gson.toJson(updatedLocalPlans)
            helper.uploadOrUpdateFile(folderId, "plans.json", jsonString)
        }
    }

    private suspend fun syncPrefs(helper: DriveServiceHelper, folderId: String) {
        val prefs = applicationContext.dataStore.data.first()
        val theme = prefs[stringPreferencesKey("theme_mode")] ?: "system"
        val language = prefs[stringPreferencesKey("language")] ?: "zh"
        val quote = prefs[stringPreferencesKey("user_quote")] ?: "坚持就是胜利"
        
        val localPrefsMap = mapOf(
            "theme_mode" to theme,
            "language" to language,
            "user_quote" to quote
        )

        val jsonString = gson.toJson(localPrefsMap)
        helper.uploadOrUpdateFile(folderId, "user_prefs.json", jsonString)
    }

    private fun transformToSetEntities(day: TrainingDay): List<SetEntity> {
        val result = mutableListOf<SetEntity>()
        day.sessions.forEach { session ->
            session.exercises.forEach { exercise ->
                exercise.sets.forEach { setRecord ->
                    val time = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .parse("${day.date} ${setRecord.time}")
                        parsed?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    result.add(
                        SetEntity(
                            date = day.date,
                            sessionId = session.sessionId,
                            exerciseName = exercise.name,
                            reps = setRecord.reps,
                            weight = setRecord.weight,
                            timestamp = time,
                            timeStr = setRecord.time,
                            remoteId = setRecord.remoteId
                        )
                    )
                }
            }
        }
        return result
    }

    private fun transformToTrainingDay(date: String, sets: List<SetEntity>): TrainingDay {
        val sessions = sets.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
            val exercises = sessionSets.groupBy { it.exerciseName }.map { (exerciseName, exerciseSets) ->
                val setRecords = exerciseSets.map { 
                    SetRecord(
                        reps = it.reps, 
                        weight = it.weight, 
                        time = it.timeStr,
                        remoteId = it.remoteId
                    ) 
                }
                ExerciseRecord(name = exerciseName, sets = setRecords)
            }
            
            val startTime = sessionSets.firstOrNull()?.timeStr ?: "00:00"
            val endTime = sessionSets.lastOrNull()?.timeStr ?: "23:59"
            
            TrainingSession(
                sessionId = sessionId,
                startTime = startTime,
                endTime = endTime,
                exercises = exercises
            )
        }
        return TrainingDay(date = date, sessions = sessions)
    }
}
