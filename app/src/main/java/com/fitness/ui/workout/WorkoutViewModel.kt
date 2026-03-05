package com.fitness.ui.workout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fitness.data.local.ExerciseDao
import com.fitness.data.local.SetEntity
import com.fitness.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ExerciseDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    // 从导航参数中获取日期，如果不存在则默认为今天
    private val navDate: String? = savedStateHandle["date"]
    private val _currentSessionId = MutableStateFlow("Session_${System.currentTimeMillis()}")
    private val _currentDate = MutableStateFlow(navDate ?: dateFormatter.format(Date()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val setsInSession: StateFlow<List<SetEntity>> = _currentDate
        .flatMapLatest { date -> dao.getSetsByDateFlow(date) }
        .combine(_currentSessionId) { list, sessionId ->
            list.filter { it.sessionId == sessionId }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val setsToday: StateFlow<List<SetEntity>> = _currentDate
        .flatMapLatest { date -> dao.getSetsByDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allHistorySets: StateFlow<List<SetEntity>> = dao.getAllSetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedExercises: StateFlow<List<String>> = setsToday.map { sets ->
        sets.map { it.exerciseName }.distinct()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun startNewSession() {
        val now = System.currentTimeMillis()
        // 只有在没有显式指定日期（即今天训练）的情况下，才随 session 更新日期
        if (navDate == null) {
            _currentDate.value = dateFormatter.format(Date(now))
        }
        _currentSessionId.value = "Session_$now"
    }

    fun addSet(exerciseId: String, weight: Double, reps: Int) {
        val now = System.currentTimeMillis()
        val dateStr = _currentDate.value // 使用当前选中的日期，而不是强制今天
        val sessionId = _currentSessionId.value
        
        val set = SetEntity(
            date = dateStr,
            sessionId = sessionId,
            exerciseName = exerciseId,
            reps = reps,
            weight = weight,
            timestamp = now,
            timeStr = timeFormatter.format(Date(now))
        )

        viewModelScope.launch {
            dao.insertSet(set)
            triggerSync(dateStr)
        }
    }

    fun updateSet(set: SetEntity) {
        viewModelScope.launch {
            dao.updateSet(set)
            triggerSync(set.date)
        }
    }

    fun deleteSet(setId: Long, date: String) {
        viewModelScope.launch {
            dao.deleteSet(setId)
            triggerSync(date)
        }
    }

    private fun triggerSync(dateStr: String) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(Data.Builder().putString("SYNC_DATE", dateStr).build())
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    suspend fun hasCompletedExercisesOnDate(dateStr: String): Boolean {
        return dao.getSetsByDate(dateStr).isNotEmpty()
    }

    suspend fun getSetsByDate(dateStr: String): List<SetEntity> {
        return dao.getSetsByDate(dateStr)
    }

    suspend fun isDayFullyCompleted(dateStr: String, routine: List<com.fitness.model.PlannedExercise>): Boolean {
        if (routine.isEmpty()) return false
        val setsOnDate = dao.getSetsByDate(dateStr)
        return routine.all { planned ->
            val completedCount = setsOnDate.count { it.exerciseName == planned.id }
            completedCount >= planned.targetSets
        }
    }
}
