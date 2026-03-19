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
import com.fitness.util.dataStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    private val json = Json { ignoreUnknownKeys = true }
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

        Log.d(TAG, "Sync: Authenticated as ${account.email}")
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccountName = account.email }

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("FitBot").build()
        
        val helper = DriveServiceHelper(driveService)
        
        try {
            Log.d(TAG, "Sync: Starting folder and file check...")
            val folderId = helper.getOrCreateFolder(FOLDER_NAME)
            Log.d(TAG, "Sync: Folder ID is $folderId")
            
            val remoteFiles = helper.queryFiles(folderId, "")
            Log.d(TAG, "Sync: Found ${remoteFiles.size} remote files.")
            val remoteFilesMap = remoteFiles.associateBy { it.name }
            
            val lastSyncTime = applicationContext.dataStore.data.first()[LAST_SYNC_KEY] ?: 0L
            Log.d(TAG, "Sync: Last sync time: $lastSyncTime")

            syncSetsLogic(helper, folderId, remoteFilesMap, lastSyncTime)
            syncPlansLogic(helper, folderId, remoteFilesMap, lastSyncTime)
            syncPrefs(helper, folderId, remoteFilesMap, lastSyncTime)

            applicationContext.dataStore.edit { it[LAST_SYNC_KEY] = System.currentTimeMillis() }
            Log.i(TAG, "Sync complete successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync: Critical error during drive operations: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncSetsLogic(helper: DriveServiceHelper, folderId: String, remoteMap: Map<String, com.google.api.services.drive.model.File>, lastSync: Long) {
        val existingIds = exerciseDao.getAllRemoteIds().toSet()

        // --- Download pass: remote files newer than lastSync ---
        val remoteSetFiles = remoteMap.keys.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.json")) }
        for (fileName in remoteSetFiles) {
            val remoteFile = remoteMap[fileName] ?: continue
            val shouldDownload = lastSync == 0L || remoteFile.modifiedTime.value > lastSync
            if (!shouldDownload) continue

            try {
                val remoteJson = helper.downloadFileById(remoteFile.id)
                val remoteDay = json.decodeFromString<TrainingDay>(remoteJson)
                transformToSetEntities(remoteDay).forEach { entity ->
                    if (!existingIds.contains(entity.remoteId)) exerciseDao.insertSet(entity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse error for $fileName: ${e.message}")
            }
        }

        // --- Upload pass: only dates with local changes since lastSync ---
        val locallyModifiedDates: Set<String> = if (lastSync == 0L) {
            exerciseDao.getDistinctDates().toSet()
        } else {
            exerciseDao.getDistinctDatesModifiedSince(lastSync).toSet()
        }

        for (date in locallyModifiedDates) {
            val localSets = exerciseDao.getSetsByDate(date)
            if (localSets.isEmpty()) continue
            val localDay = transformToTrainingDay(date, localSets)
            
            val fileName = "$date.json"
            val remoteFile = remoteMap[fileName]
            
            if (remoteFile != null) {
                try {
                    // Fetch-Merge-Upload to prevent data loss
                    val remoteJson = helper.downloadFileById(remoteFile.id)
                    val remoteDay = json.decodeFromString<TrainingDay>(remoteJson)
                    val mergedDay = mergeTrainingDays(localDay, remoteDay)
                    helper.updateFile(remoteFile.id, json.encodeToString(mergedDay))
                } catch (e: Exception) {
                    // Fallback to overwrite if remote parsing fails
                    helper.updateFile(remoteFile.id, json.encodeToString(localDay))
                }
            } else {
                helper.createFile(folderId, fileName, json.encodeToString(localDay))
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
                    planDao.insertPlan(json.decodeFromString<PlanEntity>(content).copy(id = 0))
                } catch (e: Exception) { Log.e(TAG, "Plan pull error: ${file.name}") }
            }
        }
        
        planDao.getAllPlans().forEach { plan ->
            val name = "plan_history_${plan.createdAt}.json"
            if (!remoteMap.containsKey(name)) {
                helper.createFile(folderId, name, json.encodeToString(plan))
            }
        }

        // 2. 同步当前计划 (Bidirectional - Latest Wins)
        val currentPlanFile = remoteMap["plans.json"]
        val localPlan = planDao.getCurrentPlan()
        
        if (currentPlanFile != null) {
            val remoteContent = helper.downloadFileById(currentPlanFile.id)
            
            // Determine remote timestamp from content
            var remoteTimestamp: Long = 0
            val remotePlanToInsert: PlanEntity? = try {
                val remotePlans = json.decodeFromString<List<PlanEntity>>(remoteContent)
                val bestRemote = remotePlans.firstOrNull { it.isCurrent } ?: remotePlans.maxByOrNull { it.createdAt }
                remoteTimestamp = bestRemote?.createdAt ?: 0L
                bestRemote
            } catch (e: Exception) {
                // iOS format fallback: try parsing as List<RoutineDay>
                try {
                    val routineDays = json.decodeFromString<List<com.fitness.model.RoutineDay>>(remoteContent)
                    // If no internal timestamp, fallback to Drive file's modification time
                    remoteTimestamp = currentPlanFile.modifiedTime.value
                    if (routineDays.isNotEmpty()) {
                        PlanEntity(
                            name = "Synced Routine",
                            exercisesJson = json.encodeToString(routineDays),
                            isCurrent = true,
                            version = 1,
                            createdAt = remoteTimestamp
                        )
                    } else null
                } catch (e2: Exception) {
                    Log.e(TAG, "plans.json parse failed: ${e2.message}")
                    null
                }
            }

            val localTimestamp = localPlan?.createdAt ?: 0L

            if (remoteTimestamp > localTimestamp) {
                // Remote is newer, update local
                remotePlanToInsert?.let { planDao.insertPlan(it.copy(id = 0)) }
                Log.d(TAG, "Sync: Remote plan is newer ($remoteTimestamp > $localTimestamp). Downloaded.")
            } else if (localTimestamp > remoteTimestamp) {
                // Local is newer, upload to remote
                localPlan?.let {
                    val jsonStr = json.encodeToString(listOf(it))
                    helper.updateFile(currentPlanFile.id, jsonStr)
                    Log.d(TAG, "Sync: Local plan is newer ($localTimestamp > $remoteTimestamp). Uploaded.")
                }
            } else {
                Log.d(TAG, "Sync: Plans are already in sync at $localTimestamp.")
            }
        } else {
            // No remote file, upload local
            localPlan?.let { 
                val jsonStr = json.encodeToString(listOf(it))
                helper.createFile(folderId, "plans.json", jsonStr)
                Log.d(TAG, "Sync: No remote plans.json. Uploaded local.")
            }
        }
    }

    private suspend fun syncPrefs(helper: DriveServiceHelper, folderId: String, remoteMap: Map<String, com.google.api.services.drive.model.File>, lastSync: Long) {
        val prefFile = remoteMap["user_prefs.json"]
        if (prefFile != null && prefFile.modifiedTime.value > lastSync) {
            val remoteJson = helper.downloadFileById(prefFile.id)
            val remotePrefs = json.decodeFromString<Map<String, String>>(remoteJson)
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
            val jsonStr = json.encodeToString(map)
            if (prefFile != null) helper.updateFile(prefFile.id, jsonStr)
            else helper.createFile(folderId, "user_prefs.json", jsonStr)
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
                    result.add(SetEntity(date = day.date, sessionId = session.sessionId, exerciseName = exercise.name, reps = setRecord.reps, weight = setRecord.weight, timestamp = time, timeStr = setRecord.time, remoteId = setRecord.remoteId, isDeleted = setRecord.isDeleted))
                }
            }
        }
        return result
    }

    private fun transformToTrainingDay(date: String, sets: List<SetEntity>): TrainingDay {
        val sessions = sets.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
            val exercises = sessionSets.groupBy { it.exerciseName }.map { (exerciseName, exerciseSets) ->
                val setRecords = exerciseSets.map { SetRecord(reps = it.reps, weight = it.weight, time = it.timeStr, remoteId = it.remoteId, isDeleted = it.isDeleted) }
                ExerciseRecord(name = exerciseName, sets = setRecords)
            }
            TrainingSession(sessionId = sessionId, startTime = sessionSets.firstOrNull()?.timeStr ?: "00:00", endTime = sessionSets.lastOrNull()?.timeStr ?: "23:59", exercises = exercises)
        }
        return TrainingDay(date = date, sessions = sessions)
    }
}
