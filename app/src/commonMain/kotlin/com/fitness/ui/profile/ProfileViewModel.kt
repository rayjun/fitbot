package com.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val heatmapData: StateFlow<Map<String, Int>> = repository.getHeatmapData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
