package com.fitness.sync

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fitness.data.DataStoreRepository
import com.fitness.model.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * iOS Drive sync engine.  Mirrors SyncWorker.kt logic but reads/writes data
 * from DataStoreRepository instead of Room DAOs.
 *
 * Sync rules (same as Android):
 *  - Sets    : per-day YYYY-MM-DD.json, TrainingDay JSON, exerciseName+timeStr dedup
 *  - Plans   : plans.json (WorkoutPlan wrapper containing RoutineDay list JSON), bidirectional
 *  - Prefs   : user_prefs.json, Map<String,String>, latest-wins
 */

class IosDriveSyncEngine(
    accessToken: String,
    private val repository: DataStoreRepository
) {
    private val drive = DriveApiClient(accessToken)
    private val json = Json { ignoreUnknownKeys = true }
    private val FOLDER_NAME = "FitBot"
    private val LAST_SYNC_KEY = longPreferencesKey("last_sync_time")
    private val DIRTY_DATES_KEY = stringPreferencesKey("dirty_dates")
    private val ROUTINE_MODIFIED_KEY = longPreferencesKey("workout_routine_modified")

    suspend fun sync() {
        val folderId = drive.getOrCreateFolder(FOLDER_NAME)
        val remoteFiles = drive.listFiles(folderId)
        val remoteMap: Map<String, DriveFile> = remoteFiles.associateBy { it.name }

        val lastSyncTime = repository.dataStore.data.first()[LAST_SYNC_KEY] ?: 0L

        syncSetsLogic(folderId, remoteMap, lastSyncTime)
        syncPlansLogic(folderId, remoteMap, lastSyncTime)
        syncPrefs(folderId, remoteMap, lastSyncTime)

        repository.dataStore.edit { 
            it[LAST_SYNC_KEY] = Clock.System.now().toEpochMilliseconds() 
            // Clear dirty dates after successful sync
            it.remove(DIRTY_DATES_KEY)
        }
        drive.close()
    }

    // -------------------------------------------------------------------------
    // Sets sync
    // -------------------------------------------------------------------------

    private suspend fun syncSetsLogic(
        folderId: String,
        remoteMap: Map<String, DriveFile>,
        lastSync: Long
    ) {
        val allPrefs = repository.dataStore.data.first()

        // --- Download pass: remote files newer than lastSync ---
        val remoteSetFiles = remoteMap.keys.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.json")) }
        for (fileName in remoteSetFiles) {
            val remoteFile = remoteMap[fileName] ?: continue
            val remoteModifiedMs = remoteFile.modifiedTime?.let { parseIso8601ToMs(it) } ?: 0L
            val shouldDownload = lastSync == 0L || remoteModifiedMs > lastSync
            if (!shouldDownload) continue

            try {
                val remoteJson = drive.downloadFile(remoteFile.id)
                val remoteDay = json.decodeFromString<TrainingDay>(remoteJson)
                mergeRemoteSetsIntoLocal(fileName.substringBefore(".json"), remoteDay)
            } catch (_: Exception) {}
        }

        // --- Upload pass: only dates modified locally since lastSync ---
        val locallyModifiedDates = mutableSetOf<String>()
        val dirtyJson = allPrefs[DIRTY_DATES_KEY]
        if (dirtyJson != null) {
            try {
                locallyModifiedDates.addAll(json.decodeFromString<Set<String>>(dirtyJson))
            } catch (e: Exception) {}
        }
        
        // Backwards compatibility with old LOCAL_MODIFIED_KEY_PREFIX approach
        val localModifiedPrefix = "local_modified_"
        allPrefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith(localModifiedPrefix) && value is Long) {
                if (lastSync == 0L || value > lastSync) {
                    locallyModifiedDates.add(key.name.removePrefix(localModifiedPrefix))
                }
            }
        }
        // On first sync, also upload today if there's local data
        if (lastSync == 0L) locallyModifiedDates.add(todayString())

        for (date in locallyModifiedDates) {
            val localSets = repository.getSetsByDate(date).first()
            if (localSets.isEmpty()) continue
            val localDay = buildTrainingDay(date, localSets)
            
            val fileName = "$date.json"
            val remoteFile = remoteMap[fileName]
            
            if (remoteFile != null) {
                try {
                    // Fetch-Merge-Upload
                    val remoteJson = drive.downloadFile(remoteFile.id)
                    val remoteDay = json.decodeFromString<TrainingDay>(remoteJson)
                    val mergedDay = mergeTrainingDays(localDay, remoteDay)
                    drive.updateFile(remoteFile.id, json.encodeToString(mergedDay))
                } catch (e: Exception) {
                    drive.updateFile(remoteFile.id, json.encodeToString(localDay))
                }
            } else {
                drive.createFile(folderId, fileName, json.encodeToString(localDay))
            }
        }
    }

    private suspend fun mergeRemoteSetsIntoLocal(date: String, remoteDay: TrainingDay) {
        val existing = repository.getSetsByDate(date).first()
        val existingKeys = existing.map { "${it.exerciseName}|${it.timeStr}" }.toSet()
        val existingRemoteIds = existing.map { it.remoteId }.filter { it.isNotEmpty() }.toSet()

        remoteDay.sessions.forEach { session ->
            session.exercises.forEach { exercise ->
                exercise.sets.forEach { setRecord ->
                    val dedupKey = "${exercise.name}|${setRecord.time}"
                    val alreadyExists = (setRecord.remoteId.isNotEmpty() && existingRemoteIds.contains(setRecord.remoteId))
                        || existingKeys.contains(dedupKey)
                    if (!alreadyExists) {
                        val ts = parseTimeToMs(date, setRecord.time)
                        repository.addExerciseSet(
                            ExerciseSet(
                                date = date,
                                sessionId = session.sessionId,
                                exerciseName = exercise.name,
                                reps = setRecord.reps,
                                weight = setRecord.weight,
                                timestamp = ts,
                                timeStr = setRecord.time,
                                remoteId = setRecord.remoteId,
                                isDeleted = setRecord.isDeleted
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildTrainingDay(date: String, sets: List<ExerciseSet>): TrainingDay {
        val sessions = sets.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
            val exercises = sessionSets.groupBy { it.exerciseName }.map { (name, exSets) ->
                val setRecords = exSets.map {
                    SetRecord(reps = it.reps, weight = it.weight, time = it.timeStr, remoteId = it.remoteId, isDeleted = it.isDeleted)
                }
                ExerciseRecord(name = name, sets = setRecords)
            }
            TrainingSession(
                sessionId = sessionId,
                startTime = sessionSets.firstOrNull()?.timeStr ?: "00:00",
                endTime = sessionSets.lastOrNull()?.timeStr ?: "23:59",
                exercises = exercises
            )
        }
        return TrainingDay(date = date, sessions = sessions)
    }

    // -------------------------------------------------------------------------
    // Plans sync
    // -------------------------------------------------------------------------

    private suspend fun syncPlansLogic(
        folderId: String,
        remoteMap: Map<String, DriveFile>,
        lastSync: Long
    ) {
        val plansFile = remoteMap["plans.json"]
        val localRoutine = repository.getCurrentRoutine().first()
        val localModifiedMs = repository.dataStore.data.first()[ROUTINE_MODIFIED_KEY] ?: 0L

        if (plansFile != null) {
            try {
                val content = drive.downloadFile(plansFile.id)
                
                var remoteTimestamp: Long = -1 // -1 means truly unknown
                val remoteDays: List<RoutineDay> = try {
                    val remotePlans = json.decodeFromString<List<WorkoutPlan>>(content)
                    val activePlan = remotePlans.firstOrNull { it.isCurrent }
                        ?: remotePlans.maxByOrNull { it.createdAt }
                    remoteTimestamp = activePlan?.createdAt ?: 0L
                    activePlan?.let { json.decodeFromString<List<RoutineDay>>(it.exercisesJson) } ?: emptyList()
                } catch (_: Exception) {
                    // Fallback for old formats (plain List<RoutineDay>)
                    try {
                        val days = json.decodeFromString<List<RoutineDay>>(content)
                        remoteTimestamp = plansFile.modifiedTime?.let { parseIso8601ToMs(it) } ?: 0L
                        days
                    } catch(_: Exception) { emptyList() }
                }

                if (remoteTimestamp != -1L) {
                    if (localModifiedMs == 0L || remoteTimestamp > localModifiedMs) {
                        // Remote is newer OR local is empty
                        if (remoteDays.isNotEmpty()) {
                            println("Sync: Remote plan is newer ($remoteTimestamp > $localModifiedMs). Overwriting local.")
                            repository.replaceFullRoutine(remoteDays, remoteTimestamp)
                        }
                    } else if (localModifiedMs > remoteTimestamp) {
                        // Local is newer
                        println("Sync: Local plan is newer ($localModifiedMs > $remoteTimestamp). Uploading.")
                        uploadLocalPlan(folderId, plansFile.id, localRoutine, localModifiedMs)
                    } else {
                        println("Sync: Plans are already in sync at $localModifiedMs.")
                    }
                }
            } catch (_: Exception) {}
        } else {
            // No remote file, upload local
            uploadLocalPlan(folderId, null, localRoutine, localModifiedMs)
        }
    }

    private suspend fun uploadLocalPlan(folderId: String, fileId: String?, routine: List<RoutineDay>, timestamp: Long) {
        if (routine.isEmpty()) return
        
        // Wrap into standard WorkoutPlan
        val entity = WorkoutPlan(
            name = "iOS Synced Routine",
            exercisesJson = json.encodeToString(routine),
            isCurrent = true,
            createdAt = timestamp
        )
        val jsonStr = json.encodeToString(listOf(entity))
        
        if (fileId != null) {
            drive.updateFile(fileId, jsonStr)
        } else {
            drive.createFile(folderId, "plans.json", jsonStr)
        }
    }

    // -------------------------------------------------------------------------
    // Prefs sync
    // -------------------------------------------------------------------------

    private suspend fun syncPrefs(
        folderId: String,
        remoteMap: Map<String, DriveFile>,
        lastSync: Long
    ) {
        val prefFile = remoteMap["user_prefs.json"]
        val remoteModifiedMs = prefFile?.modifiedTime?.let { parseIso8601ToMs(it) } ?: 0L

        if (prefFile != null && (lastSync == 0L || remoteModifiedMs > lastSync)) {
            try {
                val remoteJson = drive.downloadFile(prefFile.id)
                val remotePrefs = json.decodeFromString<Map<String, String>>(remoteJson)
                remotePrefs["theme_mode"]?.let { repository.setThemeMode(it) }
                remotePrefs["language"]?.let { repository.setLanguage(it) }
                remotePrefs["user_quote"]?.let { repository.setUserQuote(it) }
            } catch (_: Exception) {}
        } else {
            val themeMode = repository.getThemeMode().first()
            val language = repository.getLanguage().first()
            val userQuote = repository.getUserQuote().first()
            val map = mapOf(
                "theme_mode" to themeMode,
                "language" to language,
                "user_quote" to userQuote
            )
            val jsonStr = json.encodeToString(map)
            if (prefFile != null) {
                drive.updateFile(prefFile.id, jsonStr)
            } else {
                drive.createFile(folderId, "user_prefs.json", jsonStr)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun todayString(): String {
        val now = Clock.System.now()
        val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
        return local.date.toString()
    }

    /** Parse ISO-8601 string (e.g. "2024-01-15T10:30:00.000Z") to epoch ms. Returns 0 on failure. */
    private fun parseIso8601ToMs(iso: String): Long {
        return try {
            kotlinx.datetime.Instant.parse(iso).toEpochMilliseconds()
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseTimeToMs(date: String, timeStr: String): Long {
        return try {
            kotlinx.datetime.Instant.parse("${date}T${timeStr}:00Z").toEpochMilliseconds()
        } catch (_: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}
