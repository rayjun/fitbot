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
        Log.d(TAG, "Starting sync task. Specific date: $specificDate")
        
        // 1. 获取 Google 账户并尝试静默登录以刷新 Token
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val googleSignInClient = GoogleSignIn.getClient(applicationContext, gso)
        
        val account = try {
            // 先尝试静默登录获取最新有效 Token
            val task = googleSignInClient.silentSignIn()
            if (task.isSuccessful) {
                task.result
            } else {
                GoogleSignIn.getLastSignedInAccount(applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth failed during silent sign in", e)
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        }

        if (account == null) {
            Log.w(TAG, "No Google account found. Skipping sync.")
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
            Log.d(TAG, "Connecting to Google Drive...")
            val folderId = helper.getOrCreateFolder("MyFitnessData")
            Log.d(TAG, "Target folder ID: $folderId")

            // --- SYNC SETS ---
            val datesToSync = if (specificDate != null) {
                listOf(specificDate)
            } else {
                val dbDates = exerciseDao.getDistinctDates().toMutableSet()
                dbDates.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                dbDates.toList()
            }

            Log.d(TAG, "Syncing ${datesToSync.size} days of data...")
            datesToSync.forEach { date ->
                try {
                    syncSets(helper, folderId, date)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync sets for date: $date", e)
                }
            }

            // --- SYNC PLANS ---
            Log.d(TAG, "Syncing training plans...")
            syncPlans(helper, folderId)

            // --- SYNC PREFS ---
            Log.d(TAG, "Syncing user preferences...")
            syncPrefs(helper, folderId)

            Log.i(TAG, "Full sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync process failed", e)
            Result.retry()
        }
    }

    private suspend fun syncSets(helper: DriveServiceHelper, folderId: String, date: String) {
        val fileName = "$date.json"
        Log.v(TAG, "Processing file: $fileName")
        
        val remoteJson = helper.downloadFile(folderId, fileName)
        val existingRemoteIds = exerciseDao.getAllRemoteIds().toSet()

        if (remoteJson != null) {
            try {
                val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                val remoteSets = transformToSetEntities(remoteDay)
                var mergedCount = 0
                remoteSets.forEach { remoteSet ->
                    if (!existingRemoteIds.contains(remoteSet.remoteId)) {
                        exerciseDao.insertSet(remoteSet)
                        mergedCount++
                    }
                }
                if (mergedCount > 0) Log.d(TAG, "Merged $mergedCount new sets from cloud for $date")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing remote JSON for $date", e)
            }
        }

        val allSets = exerciseDao.getSetsByDate(date)
        if (allSets.isNotEmpty()) {
            val trainingDay = transformToTrainingDay(date, allSets)
            val jsonString = gson.toJson(trainingDay)
            helper.uploadOrUpdateFile(folderId, fileName, jsonString)
            Log.v(TAG, "Uploaded ${allSets.size} sets for $date")
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
                        Log.d(TAG, "Updated local plan: ${remotePlan.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing plans.json", e)
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
