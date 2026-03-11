package com.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.AnalyticsEngine
import com.fitness.data.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val heatmapData: StateFlow<Map<String, Int>> = repository.getHeatmapData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val muscleVolumeData: StateFlow<Map<String, Double>> = repository.getAllSets()
        .map { sets -> AnalyticsEngine.calculateVolumePerMuscleGroup(sets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
