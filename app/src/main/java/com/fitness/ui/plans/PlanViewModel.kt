package com.fitness.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.local.PlanDao
import com.fitness.data.local.PlanEntity
import com.fitness.model.PlannedExercise
import com.fitness.model.RoutineDay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val dao: PlanDao
) : ViewModel() {
    private val gson = Gson()

    private val _currentPlan = MutableStateFlow<PlanEntity?>(null)
    val currentPlan: StateFlow<PlanEntity?> = _currentPlan

    // Add state flow for parsed routine
    val currentRoutine: StateFlow<List<RoutineDay>> = _currentPlan.map { plan ->
        if (plan == null) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<RoutineDay>>() {}.type
                gson.fromJson<List<RoutineDay>>(plan.exercisesJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allPlans = MutableStateFlow<List<PlanEntity>>(emptyList())
    val allPlans: StateFlow<List<PlanEntity>> = _allPlans

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _currentPlan.value = dao.getCurrentPlan()
            _allPlans.value = dao.getAllPlans()
        }
    }

    suspend fun getRoutineForTimestamp(timestamp: Long): List<RoutineDay> {
        // 1. 查找在该时间点之前（包含）最后创建的计划
        val plan = dao.getPlanForTimestamp(timestamp)
        
        if (plan != null) {
            return try {
                val type = object : TypeToken<List<RoutineDay>>() {}.type
                gson.fromJson<List<RoutineDay>>(plan.exercisesJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        // 2. 如果没找到，检查该日期是否在第一份计划创建之后
        // 如果该日期早于第一份计划的创建时间，说明那时根本没有计划，返回空
        return emptyList()
    }

    fun updatePlan(name: String, routine: List<RoutineDay>) {
        val version = (_currentPlan.value?.version ?: 0) + 1
        val newPlan = PlanEntity(
            name = name,
            exercisesJson = gson.toJson(routine),
            isCurrent = true,
            version = version,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            dao.updatePlan(newPlan)
            refresh()
        }
    }

    fun updatePlanDay(dayOfWeek: Int, isRest: Boolean, exercises: List<PlannedExercise>) {
        val current = currentRoutine.value.toMutableList()
        val index = current.indexOfFirst { it.dayOfWeek == dayOfWeek }
        if (index != -1) {
            current[index] = current[index].copy(isRest = isRest, exercises = exercises)
        } else {
            current.add(RoutineDay(dayOfWeek, isRest, exercises))
        }
        updatePlan(currentPlan.value?.name ?: "Daily Routine", current)
    }
}
