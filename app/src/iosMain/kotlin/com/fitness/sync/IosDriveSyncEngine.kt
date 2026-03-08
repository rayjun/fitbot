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
 *  - Plans   : plans.json (PlanEntity wrapper containing RoutineDay list JSON), bidirectional
 *  - Prefs   : user_prefs.json, Map<String,String>, latest-wins
 */

/** Mirrors Android's PlanEntity for deserializing plans.json written by Android. */
@Serializable
private data class PlanEntity(
    val id: Long = 0,
    val name: String = "",
    val exercisesJson: String = "[]",
    val isCurrent: Boolean = true,
    val version: Int = 0,
    val createdAt: Long = 0
)

class IosDriveSyncEngine(
    accessToken: String,
    private val repository: DataStoreRepository
) {
    private val drive = DriveApiClient(accessToken)
    private val json = Json { ignoreUnknownKeys = true }
    private val FOLDER_NAME = "FitBot"
    private val LAST_SYNC_KEY = longPreferencesKey("last_sync_time")
    private val LOCAL_MODIFIED_KEY_PREFIX = "local_modified_"

    suspend fun sync() {
        val folderId = drive.getOrCreateFolder(FOLDER_NAME)
        val remoteFiles = drive.listFiles(folderId)
        val remoteMap: Map<String, DriveFile> = remoteFiles.associateBy { it.name }

        val lastSyncTime = repository.dataStore.data.first()[LAST_SYNC_KEY] ?: 0L

        syncSetsLogic(folderId, remoteMap, lastSyncTime)
        syncPlansLogic(folderId, remoteMap, lastSyncTime)
        syncPrefs(folderId, remoteMap, lastSyncTime)

        repository.dataStore.edit { it[LAST_SYNC_KEY] = Clock.System.now().toEpochMilliseconds() }
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
        allPrefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith(LOCAL_MODIFIED_KEY_PREFIX) && value is Long) {
                if (lastSync == 0L || value > lastSync) {
                    locallyModifiedDates.add(key.name.removePrefix(LOCAL_MODIFIED_KEY_PREFIX))
                }
            }
        }
        // On first sync, also upload today if there's local data
        if (lastSync == 0L) locallyModifiedDates.add(todayString())

        for (date in locallyModifiedDates) {
            val localSets = repository.getSetsByDate(date).first()
            if (localSets.isEmpty()) continue
            val localDay = buildTrainingDay(date, localSets)
            val localJsonStr = json.encodeToString(localDay)
            val fileName = "$date.json"
            val remoteFile = remoteMap[fileName]
            if (remoteFile != null) {
                drive.updateFile(remoteFile.id, localJsonStr)
            } else {
                drive.createFile(folderId, fileName, localJsonStr)
            }
        }
    }

    /**
     * Merges remote sets into local DataStore.
     * Deduplication key: exerciseName + timeStr (remoteId may be empty for older Android records).
     */
    private suspend fun mergeRemoteSetsIntoLocal(date: String, remoteDay: TrainingDay) {
        val existing = repository.getSetsByDate(date).first()
        // Build a set of "exerciseName|timeStr" keys for dedup
        val existingKeys = existing.map { "${it.exerciseName}|${it.timeStr}" }.toSet()
        val existingRemoteIds = existing.map { it.remoteId }.filter { it.isNotEmpty() }.toSet()

        remoteDay.sessions.forEach { session ->
            session.exercises.forEach { exercise ->
                exercise.sets.forEach { setRecord ->
                    val dedupKey = "${exercise.name}|${setRecord.time}"
                    // Skip if we already have this record (by remoteId or by name+time)
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
                                remoteId = setRecord.remoteId
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
                    SetRecord(reps = it.reps, weight = it.weight, time = it.timeStr, remoteId = it.remoteId)
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
        val remoteModifiedMs = plansFile?.modifiedTime?.let { parseIso8601ToMs(it) } ?: 0L

        // Pull remote when first sync or remote is newer
        if (plansFile != null && (lastSync == 0L || remoteModifiedMs > lastSync)) {
            try {
                val content = drive.downloadFile(plansFile.id)
                // plans.json is written by Android as List<PlanEntity> where
                // PlanEntity.exercisesJson contains the List<RoutineDay> as JSON string.
                // Try that format first, fall back to plain List<RoutineDay>.
                val remoteDays: List<RoutineDay> = try {
                    val planEntities = json.decodeFromString<List<PlanEntity>>(content)
                    // Use the first isCurrent plan, or the latest by createdAt
                    val activePlan = planEntities.firstOrNull { it.isCurrent }
                        ?: planEntities.maxByOrNull { it.createdAt }
                    activePlan?.let { json.decodeFromString<List<RoutineDay>>(it.exercisesJson) } ?: emptyList()
                } catch (_: Exception) {
                    // Fallback: treat as plain List<RoutineDay> (iOS-written format)
                    json.decodeFromString<List<RoutineDay>>(content)
                }

                if (remoteDays.isNotEmpty()) {
                    remoteDays.forEach { day ->
                        repository.updateRoutineDay(day.dayOfWeek, day.isRest, day.exercises)
                    }
                }
            } catch (_: Exception) {}
        } else {
            // Push local routine to remote (iOS uses plain List<RoutineDay>)
            val localRoutine = repository.getCurrentRoutine().first()
            if (localRoutine.isNotEmpty()) {
                val jsonStr = json.encodeToString(localRoutine)
                if (plansFile != null) {
                    drive.updateFile(plansFile.id, jsonStr)
                } else {
                    drive.createFile(folderId, "plans.json", jsonStr)
                }
            }
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
