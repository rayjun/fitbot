package com.fitness.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fitness.model.ExerciseSet
import com.fitness.model.PlannedExercise
import com.fitness.model.RoutineDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreRepository(
    val dataStore: DataStore<Preferences>
) : WorkoutRepository, SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }

    // --- Keys ---
    private val ROUTINE_KEY = stringPreferencesKey("workout_routine")
    private val HISTORY_KEY_PREFIX = "history_"
    private val LOCAL_MODIFIED_KEY_PREFIX = "local_modified_"
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val USER_QUOTE_KEY = stringPreferencesKey("user_quote")

    // --- WorkoutRepository ---

    override fun getCurrentRoutine(): Flow<List<RoutineDay>> {
        return dataStore.data.map { preferences ->
            val jsonStr = preferences[ROUTINE_KEY]
            if (jsonStr == null) {
                // Default routine if empty
                listOf(
                    RoutineDay(1, false, listOf(PlannedExercise("benchpress", 3))),
                    RoutineDay(2, false, listOf(PlannedExercise("squat", 3))),
                    RoutineDay(3, true, emptyList()),
                    RoutineDay(4, false, listOf(PlannedExercise("pullup", 3))),
                    RoutineDay(5, false, listOf(PlannedExercise("overhead_press", 3))),
                    RoutineDay(6, false, listOf(PlannedExercise("deadlift", 3))),
                    RoutineDay(7, true, emptyList())
                )
            } else {
                try {
                    json.decodeFromString<List<RoutineDay>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override suspend fun updateRoutineDay(dayOfWeek: Int, isRest: Boolean, exercises: List<PlannedExercise>) {
        dataStore.edit { preferences ->
            val currentJson = preferences[ROUTINE_KEY]
            val currentList = if (currentJson != null) {
                try { json.decodeFromString<List<RoutineDay>>(currentJson).toMutableList() } catch(e: Exception) { mutableListOf() }
            } else {
                mutableListOf()
            }

            val index = currentList.indexOfFirst { it.dayOfWeek == dayOfWeek }
            if (index != -1) {
                currentList[index] = currentList[index].copy(isRest = isRest, exercises = exercises)
            } else {
                currentList.add(RoutineDay(dayOfWeek, isRest, exercises))
            }

            preferences[ROUTINE_KEY] = json.encodeToString(currentList)
        }
    }

    override fun getHeatmapData(): Flow<Map<String, Int>> {
        return dataStore.data.map { preferences ->
            val result = mutableMapOf<String, Int>()
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith(HISTORY_KEY_PREFIX) && value is String) {
                    val date = key.name.removePrefix(HISTORY_KEY_PREFIX)
                    try {
                        val sets = json.decodeFromString<List<ExerciseSet>>(value).filter { !it.isDeleted }
                        if (sets.isNotEmpty()) result[date] = sets.size
                    } catch (_: Exception) {}
                }
            }
            result
        }
    }

    override fun getAllSets(): Flow<List<ExerciseSet>> {
        return dataStore.data.map { preferences ->
            val result = mutableListOf<ExerciseSet>()
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith(HISTORY_KEY_PREFIX) && value is String) {
                    try {
                        val sets = json.decodeFromString<List<ExerciseSet>>(value)
                        result.addAll(sets)
                    } catch (_: Exception) {}
                }
            }
            result
        }
    }

    override fun getSetsByDate(date: String): Flow<List<ExerciseSet>> {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + date)
        return dataStore.data.map { preferences ->
            val jsonStr = preferences[key]
            if (jsonStr != null) {
                try { json.decodeFromString<List<ExerciseSet>>(jsonStr).filter { !it.isDeleted } } catch(e: Exception) { emptyList() }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun addExerciseSet(set: ExerciseSet) {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + set.date)
        val modifiedKey = longPreferencesKey(LOCAL_MODIFIED_KEY_PREFIX + set.date)
        dataStore.edit { preferences ->
            val currentJson = preferences[key]
            val currentList = if (currentJson != null) {
                try { json.decodeFromString<List<ExerciseSet>>(currentJson).toMutableList() } catch(e: Exception) { mutableListOf() }
            } else {
                mutableListOf()
            }
            // Use UUID for robust distributed sync. If remoteId is already set, keep it.
            val finalRemoteId = if (set.remoteId.isEmpty()) "ios-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}" else set.remoteId
            val newSet = if (set.id == 0L) set.copy(id = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), remoteId = finalRemoteId) else set.copy(remoteId = finalRemoteId)
            currentList.add(newSet)
            preferences[key] = json.encodeToString(currentList)
            preferences[modifiedKey] = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
    }

    override suspend fun updateExerciseSet(set: ExerciseSet) {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + set.date)
        val modifiedKey = longPreferencesKey(LOCAL_MODIFIED_KEY_PREFIX + set.date)
        dataStore.edit { preferences ->
            val currentJson = preferences[key]
            if (currentJson != null) {
                try {
                    val currentList = json.decodeFromString<List<ExerciseSet>>(currentJson).toMutableList()
                    val index = currentList.indexOfFirst { it.id == set.id }
                    if (index != -1) {
                        currentList[index] = set
                        preferences[key] = json.encodeToString(currentList)
                        preferences[modifiedKey] = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    override suspend fun deleteExerciseSet(setId: Long, date: String) {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + date)
        val modifiedKey = longPreferencesKey(LOCAL_MODIFIED_KEY_PREFIX + date)
        dataStore.edit { preferences ->
            val currentJson = preferences[key]
            if (currentJson != null) {
                try {
                    val currentList = json.decodeFromString<List<ExerciseSet>>(currentJson).toMutableList()
                    val index = currentList.indexOfFirst { it.id == setId }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(isDeleted = true)
                        preferences[key] = json.encodeToString(currentList)
                        preferences[modifiedKey] = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    // --- SettingsRepository ---

    override fun getThemeMode(): Flow<String> = dataStore.data.map { it[THEME_MODE_KEY] ?: "system" }
    override suspend fun setThemeMode(mode: String) { dataStore.edit { it[THEME_MODE_KEY] = mode } }

    override fun getLanguage(): Flow<String> = dataStore.data.map { it[LANGUAGE_KEY] ?: "en" }
    override suspend fun setLanguage(lang: String) { dataStore.edit { it[LANGUAGE_KEY] = lang } }

    override fun getUserQuote(): Flow<String> = dataStore.data.map { it[USER_QUOTE_KEY] ?: "Stay fit with FitBot" }
    override suspend fun setUserQuote(quote: String) { dataStore.edit { it[USER_QUOTE_KEY] = quote } }
}
