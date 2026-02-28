package com.fitness.ui.plans

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.local.AppDatabase
import com.fitness.data.local.PlanEntity
import com.fitness.model.RoutineDay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.planDao()
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

    fun updatePlanDay(dayOfWeek: Int, isRest: Boolean, exerciseIds: List<String>) {
        val current = currentRoutine.value.toMutableList()
        val index = current.indexOfFirst { it.dayOfWeek == dayOfWeek }
        if (index != -1) {
            current[index] = current[index].copy(isRest = isRest, exercises = exerciseIds)
        } else {
            current.add(RoutineDay(dayOfWeek, isRest, exerciseIds))
        }
        updatePlan(currentPlan.value?.name ?: "Weekly Routine", current)
    }
}
