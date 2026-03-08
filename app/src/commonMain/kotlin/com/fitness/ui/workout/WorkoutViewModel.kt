package com.fitness.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.WorkoutRepository
import com.fitness.model.ExerciseSet
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _currentDate = MutableStateFlow(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val setsToday: StateFlow<List<ExerciseSet>> = _currentDate
        .flatMapLatest { date -> repository.getSetsByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDate(date: String) {
        _currentDate.value = date
    }

    fun addSet(exerciseId: String, weight: Double, reps: Int) {
        viewModelScope.launch {
            val now = Clock.System.now()
            val localTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val timeStr = "${localTime.hour.toString().padStart(2, '0')}:${localTime.minute.toString().padStart(2, '0')}"
            
            val set = ExerciseSet(
                date = _currentDate.value,
                sessionId = "Session_${now.toEpochMilliseconds()}",
                exerciseName = exerciseId,
                reps = reps,
                weight = weight,
                timestamp = now.toEpochMilliseconds(),
                timeStr = timeStr
            )
            repository.addExerciseSet(set)
        }
    }

    fun updateSet(set: ExerciseSet) {
        viewModelScope.launch {
            repository.updateExerciseSet(set)
        }
    }

    fun deleteSet(setId: Long, date: String) {
        viewModelScope.launch {
            repository.deleteExerciseSet(setId, date)
        }
    }
}
