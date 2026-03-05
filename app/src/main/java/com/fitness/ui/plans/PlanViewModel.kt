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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val dao: PlanDao
) : ViewModel() {
    private val gson = Gson()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _currentPlan = MutableStateFlow<PlanEntity?>(null)
    val currentPlan: StateFlow<PlanEntity?> = _currentPlan

    val currentRoutine: StateFlow<List<RoutineDay>> = _currentPlan.map { plan ->
        if (plan == null) emptyList()
        else try {
            val type = object : TypeToken<List<RoutineDay>>() {}.type
            gson.fromJson<List<RoutineDay>>(plan.exercisesJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allPlans = MutableStateFlow<List<PlanEntity>>(emptyList())
    val allPlans: StateFlow<List<PlanEntity>> = _allPlans

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _currentPlan.value = dao.getCurrentPlan()
            _allPlans.value = dao.getAllPlans()
        }
    }

    /**
     * 获取指定时间点的计划快照
     */
    suspend fun getRoutineForTimestamp(timestamp: Long): List<RoutineDay> {
        val allLocalPlans = dao.getAllPlans()
        if (allLocalPlans.isEmpty()) return emptyList()

        // 逻辑优化：查找在该时间点之前（包含）最后创建的计划
        val historicalPlan = dao.getPlanForTimestamp(timestamp)
        
        // 体验优化：如果该日期早于所有计划的创建时间，
        // 我们不再返回空，而是返回“第一份计划”，作为用户的初始参考。
        val planToParse = historicalPlan ?: allLocalPlans.lastOrNull()

        return if (planToParse == null) emptyList()
        else try {
            val type = object : TypeToken<List<RoutineDay>>() {}.type
            gson.fromJson<List<RoutineDay>>(planToParse.exercisesJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /**
     * 更新计划逻辑：增加“同日合并”防止版本爆炸
     */
    fun updatePlan(name: String, routine: List<RoutineDay>) {
        viewModelScope.launch {
            val current = dao.getCurrentPlan()
            val now = System.currentTimeMillis()
            val todayStr = dateFormatter.format(Date(now))
            
            // 如果最新的一份计划也是今天创建的，则直接更新它，不再产生新文件
            val lastCreatedDayStr = current?.let { dateFormatter.format(Date(it.createdAt)) }
            
            if (current != null && lastCreatedDayStr == todayStr) {
                val updatedPlan = current.copy(
                    name = name,
                    exercisesJson = gson.toJson(routine),
                    createdAt = now // 更新时间戳
                )
                dao.insertPlan(updatedPlan) // REPLACE 冲突策略会覆盖 ID 相同的记录
            } else {
                // 产生新版本
                val version = (current?.version ?: 0) + 1
                val newPlan = PlanEntity(
                    name = name,
                    exercisesJson = gson.toJson(routine),
                    isCurrent = true,
                    version = version,
                    createdAt = now
                )
                dao.updatePlan(newPlan) // 内部包含 archive 逻辑
            }
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
