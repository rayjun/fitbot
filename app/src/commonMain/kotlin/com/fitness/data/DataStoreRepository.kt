package com.fitness.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fitness.model.ExerciseSet
import com.fitness.model.PlannedExercise
import com.fitness.model.RoutineDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreRepository(
    private val dataStore: DataStore<Preferences>
) : WorkoutRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val ROUTINE_KEY = stringPreferencesKey("workout_routine")
    private val HISTORY_KEY_PREFIX = "history_"

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
        // Simplified for now: just return empty map or implement scanning all history keys
        return dataStore.data.map { emptyMap() }
    }

    override fun getSetsByDate(date: String): Flow<List<ExerciseSet>> {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + date)
        return dataStore.data.map { preferences ->
            val jsonStr = preferences[key]
            if (jsonStr != null) {
                try { json.decodeFromString<List<ExerciseSet>>(jsonStr) } catch(e: Exception) { emptyList() }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun addExerciseSet(set: ExerciseSet) {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + set.date)
        dataStore.edit { preferences ->
            val currentJson = preferences[key]
            val currentList = if (currentJson != null) {
                try { json.decodeFromString<List<ExerciseSet>>(currentJson).toMutableList() } catch(e: Exception) { mutableListOf() }
            } else {
                mutableListOf()
            }
            val newSet = if (set.id == 0L) set.copy(id = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) else set
            currentList.add(newSet)
            preferences[key] = json.encodeToString(currentList)
        }
    }

    override suspend fun updateExerciseSet(set: ExerciseSet) {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + set.date)
        dataStore.edit { preferences ->
            val currentJson = preferences[key]
            if (currentJson != null) {
                try { 
                    val currentList = json.decodeFromString<List<ExerciseSet>>(currentJson).toMutableList()
                    val index = currentList.indexOfFirst { it.id == set.id }
                    if (index != -1) {
                        currentList[index] = set
                        preferences[key] = json.encodeToString(currentList)
                    }
                } catch(e: Exception) {}
            }
        }
    }

    override suspend fun deleteExerciseSet(setId: Long, date: String) {
        val key = stringPreferencesKey(HISTORY_KEY_PREFIX + date)
        dataStore.edit { preferences ->
            val currentJson = preferences[key]
            if (currentJson != null) {
                try { 
                    val currentList = json.decodeFromString<List<ExerciseSet>>(currentJson).toMutableList()
                    currentList.removeAll { it.id == setId }
                    preferences[key] = json.encodeToString(currentList)
                } catch(e: Exception) {}
            }
        }
    }
}
