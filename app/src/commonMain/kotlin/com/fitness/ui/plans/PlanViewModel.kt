package com.fitness.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.WorkoutRepository
import com.fitness.model.PlannedExercise
import com.fitness.model.RoutineDay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val currentRoutine: StateFlow<List<RoutineDay>> = repository.getCurrentRoutine()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updatePlanDay(dayOfWeek: Int, isRest: Boolean, exercises: List<PlannedExercise>) {
        viewModelScope.launch {
            repository.updateRoutineDay(dayOfWeek, isRest, exercises)
        }
    }
}
