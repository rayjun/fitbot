package com.fitness.sync

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitness.data.local.AppDatabase
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
import java.text.SimpleDateFormat
import java.util.*

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val date = inputData.getString("SYNC_DATE") ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // 1. 获取已登录的 Google 账户
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            ?: return@withContext Result.failure()

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
        ).setApplicationName("Fitness Tracker").build()

        val helper = DriveServiceHelper(driveService)
        val db = AppDatabase.getInstance(applicationContext)
        
        try {
            val folderId = helper.getOrCreateFolder("MyFitnessData")

            // --- SYNC SETS ---
            syncSets(helper, db, folderId, date)

            // --- SYNC PLANS ---
            syncPlans(helper, db, folderId)

            // --- SYNC PREFS ---
            syncPrefs(helper, folderId)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun syncSets(helper: DriveServiceHelper, db: AppDatabase, folderId: String, date: String) {
        val dao = db.exerciseDao()
        val remoteJson = helper.downloadFile(folderId, "$date.json")
        val existingRemoteIds = dao.getAllRemoteIds().toSet()

        if (remoteJson != null) {
            val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
            val remoteSets = transformToSetEntities(remoteDay)
            remoteSets.forEach { remoteSet ->
                if (!existingRemoteIds.contains(remoteSet.remoteId)) {
                    dao.insertSet(remoteSet)
                }
            }
        }

        val allSets = dao.getSetsByDate(date)
        if (allSets.isNotEmpty()) {
            val trainingDay = transformToTrainingDay(date, allSets)
            val jsonString = gson.toJson(trainingDay)
            helper.uploadOrUpdateFile(folderId, "$date.json", jsonString)
        }
    }

    private suspend fun syncPlans(helper: DriveServiceHelper, db: AppDatabase, folderId: String) {
        val dao = db.planDao()
        val remoteJson = helper.downloadFile(folderId, "plans.json")
        
        if (remoteJson != null) {
            val type = object : TypeToken<List<PlanEntity>>() {}.type
            val remotePlans: List<PlanEntity> = gson.fromJson(remoteJson, type) ?: emptyList()
            val localPlans = dao.getAllPlans().associateBy { it.id }
            
            // Simple merge: remote wins if version is higher or doesn't exist locally
            remotePlans.forEach { remotePlan ->
                val localPlan = localPlans[remotePlan.id]
                if (localPlan == null || remotePlan.version > localPlan.version) {
                    dao.insertPlan(remotePlan)
                }
            }
        }
        
        val updatedLocalPlans = dao.getAllPlans()
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

        // For simplicity, local always overwrites remote in this first pass, 
        // as proper two-way sync on prefs requires last_modified timestamps.
        val jsonString = gson.toJson(localPrefsMap)
        helper.uploadOrUpdateFile(folderId, "user_prefs.json", jsonString)
    }

    /**
     * 将层级化的 TrainingDay 还原为扁平化的 SetEntity 列表
     */
    private fun transformToSetEntities(day: TrainingDay): List<SetEntity> {
        val result = mutableListOf<SetEntity>()
        day.sessions.forEach { session ->
            session.exercises.forEach { exercise ->
                exercise.sets.forEach { setRecord ->
                    val time = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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

    /**
     * 将扁平化的 SetEntity 列表转换为层级化的 TrainingDay
     */
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
