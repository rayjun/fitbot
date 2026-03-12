package com.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.AnalyticsEngine
import com.fitness.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

enum class TimeRange {
    WEEK, MONTH, YEAR
}

class ProfileViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val heatmapData: StateFlow<Map<String, Int>> = repository.getHeatmapData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    val muscleVolumeData: StateFlow<Map<String, Double>> = combine(
        repository.getAllSets(),
        _selectedCategory,
        _selectedTimeRange
    ) { sets, category, range ->
        val now = Clock.System.now()
        val filterDate = when (range) {
            TimeRange.WEEK -> now.minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            TimeRange.MONTH -> now.minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            TimeRange.YEAR -> now.minus(365, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        }.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

        val filteredSets = sets.filter { it.date >= filterDate }
        val aggregated = AnalyticsEngine.calculateVolumePerMuscleGroup(filteredSets)
        
        if (category == null) {
            aggregated
        } else {
            aggregated.filter { it.key == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSelectedTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }
}
