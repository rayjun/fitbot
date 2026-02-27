package com.fitness.ui.plans

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.local.AppDatabase
import com.fitness.data.local.PlanEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlanViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.planDao()

    private val _currentPlan = MutableStateFlow<PlanEntity?>(null)
    val currentPlan: StateFlow<PlanEntity?> = _currentPlan

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

    fun updatePlan(name: String, exerciseIds: List<String>) {
        val version = (_currentPlan.value?.version ?: 0) + 1
        val newPlan = PlanEntity(
            name = name,
            exercisesJson = exerciseIds.joinToString(","),
            isCurrent = true,
            version = version,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            dao.updatePlan(newPlan)
            refresh()
        }
    }
}
