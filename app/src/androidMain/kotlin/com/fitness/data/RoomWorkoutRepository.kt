package com.fitness.data

import com.fitness.data.local.ExerciseDao
import com.fitness.data.local.PlanDao
import com.fitness.data.local.SetEntity
import com.fitness.data.local.toModel
import com.fitness.model.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.*

class RoomWorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val planDao: PlanDao,
    private val dataStore: DataStore<Preferences>
) : WorkoutRepository, SettingsRepository {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override fun getCurrentRoutine(): Flow<List<RoutineDay>> = planDao.getCurrentPlanFlow().map { plan ->
        if (plan == null) emptyList()
        else try {
            json.decodeFromString<List<RoutineDay>>(plan.exercisesJson)
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun updateRoutineDay(dayOfWeek: Int, isRest: Boolean, exercises: List<PlannedExercise>) {
        val currentPlan = planDao.getCurrentPlan()
        val routine = if (currentPlan == null) mutableListOf() else {
            try { json.decodeFromString<List<RoutineDay>>(currentPlan.exercisesJson).toMutableList() } catch(e: Exception) { mutableListOf() }
        }
        
        val index = routine.indexOfFirst { it.dayOfWeek == dayOfWeek }
        if (index != -1) {
            routine[index] = routine[index].copy(isRest = isRest, exercises = exercises)
        } else {
            routine.add(RoutineDay(dayOfWeek, isRest, exercises))
        }

        val now = System.currentTimeMillis()
        if (currentPlan != null && isSameDay(currentPlan.createdAt, now)) {
            planDao.insertPlan(currentPlan.copy(exercisesJson = json.encodeToString(routine), createdAt = now))
        } else {
            val newPlan = com.fitness.data.local.PlanEntity(
                name = "Daily Routine",
                exercisesJson = json.encodeToString(routine),
                isCurrent = true,
                version = (currentPlan?.version ?: 0) + 1,
                createdAt = now
            )
            planDao.updatePlan(newPlan)
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }

    override fun getHeatmapData(): Flow<Map<String, Int>> = exerciseDao.getAllSetsFlow().map { sets ->
        sets.groupBy { it.date }.mapValues { it.value.size }
    }

    override fun getSetsByDate(date: String): Flow<List<ExerciseSet>> = exerciseDao.getSetsByDateFlow(date).map { list ->
        list.map { it.toModel() }
    }

    override suspend fun addExerciseSet(set: ExerciseSet) {
        exerciseDao.insertSet(SetEntity(
            date = set.date,
            sessionId = set.sessionId,
            exerciseName = set.exerciseName,
            reps = set.reps,
            weight = set.weight,
            timestamp = set.timestamp,
            timeStr = set.timeStr
        ))
    }

    override suspend fun updateExerciseSet(set: ExerciseSet) {
        exerciseDao.updateSet(SetEntity(
            id = set.id,
            date = set.date,
            sessionId = set.sessionId,
            exerciseName = set.exerciseName,
            reps = set.reps,
            weight = set.weight,
            timestamp = set.timestamp,
            timeStr = set.timeStr
        ))
    }

    override suspend fun deleteExerciseSet(setId: Long, date: String) {
        exerciseDao.deleteSet(setId)
    }

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val USER_QUOTE_KEY = stringPreferencesKey("user_quote")

    override fun getThemeMode(): Flow<String> = dataStore.data.map { it[THEME_MODE_KEY] ?: "system" }
    override suspend fun setThemeMode(mode: String) { dataStore.edit { it[THEME_MODE_KEY] = mode } }
    override fun getLanguage(): Flow<String> = dataStore.data.map { it[LANGUAGE_KEY] ?: "en" }
    override suspend fun setLanguage(lang: String) { dataStore.edit { it[LANGUAGE_KEY] = lang } }
    override fun getUserQuote(): Flow<String> = dataStore.data.map { it[USER_QUOTE_KEY] ?: "Stay fit with FitBot" }
    override suspend fun setUserQuote(quote: String) { dataStore.edit { it[USER_QUOTE_KEY] = quote } }
}
