package com.fitness.data

import com.fitness.model.ExerciseSet
import com.fitness.model.PlannedExercise
import com.fitness.model.RoutineDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkoutRepository : WorkoutRepository, SettingsRepository {
    private val routine = MutableStateFlow<List<RoutineDay>>(emptyList())
    private val sets = MutableStateFlow<List<ExerciseSet>>(emptyList())
    
    private val themeMode = MutableStateFlow("system")
    private val language = MutableStateFlow("en")
    private val userQuote = MutableStateFlow("Stay fit with FitBot")
    private val aiApiKey = MutableStateFlow("")
    private val aiBaseUrl = MutableStateFlow("https://api.openai.com/v1")
    private val aiModel = MutableStateFlow("gpt-3.5-turbo")

    override fun getCurrentRoutine(): Flow<List<RoutineDay>> = routine

    override suspend fun updateRoutineDay(dayOfWeek: Int, isRest: Boolean, exercises: List<PlannedExercise>) {
        val current = routine.value.toMutableList()
        val index = current.indexOfFirst { it.dayOfWeek == dayOfWeek }
        val newDay = RoutineDay(dayOfWeek, isRest, exercises)
        if (index != -1) {
            current[index] = newDay
        } else {
            current.add(newDay)
        }
        routine.value = current
    }

    override fun getHeatmapData(): Flow<Map<String, Int>> = sets.map { list ->
        list.groupBy { it.date }.mapValues { it.value.size }
    }

    override fun getAllSets(): Flow<List<ExerciseSet>> = sets

    override fun getSetsByDate(date: String): Flow<List<ExerciseSet>> = sets.map { list ->
        list.filter { it.date == date && !it.isDeleted }
    }

    override suspend fun addExerciseSet(set: ExerciseSet) {
        sets.value = sets.value + set
    }

    override suspend fun updateExerciseSet(set: ExerciseSet) {
        sets.value = sets.value.map { if (it.id == set.id) set else it }
    }

    override suspend fun deleteExerciseSet(setId: Long, date: String) {
        sets.value = sets.value.map { if (it.id == setId) it.copy(isDeleted = true) else it }
    }

    // SettingsRepository implementation
    override fun getThemeMode(): Flow<String> = themeMode
    override suspend fun setThemeMode(mode: String) { themeMode.value = mode }
    override fun getLanguage(): Flow<String> = language
    override suspend fun setLanguage(lang: String) { language.value = lang }
    override fun getUserQuote(): Flow<String> = userQuote
    override suspend fun setUserQuote(quote: String) { userQuote.value = quote }

    override fun getAiApiKey(): Flow<String> = aiApiKey
    override suspend fun setAiApiKey(key: String) { aiApiKey.value = key }
    override fun getAiBaseUrl(): Flow<String> = aiBaseUrl
    override suspend fun setAiBaseUrl(url: String) { aiBaseUrl.value = url }
    override fun getAiModel(): Flow<String> = aiModel
    override suspend fun setAiModel(model: String) { aiModel.value = model }
}
