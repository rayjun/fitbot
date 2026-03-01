package com.fitness.ui.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fitness.data.local.AppDatabase
import com.fitness.data.local.SetEntity
import com.fitness.sync.SyncWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkoutViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.exerciseDao()

    // 使用稳定 Locale 进行日期格式化，防止数据库查询 Key 变化
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    // 状态流
    private val _currentSessionId = MutableStateFlow("Session_${System.currentTimeMillis()}")
    private val _currentDate = MutableStateFlow(dateFormatter.format(Date()))

    // 响应式数据源：当日期或 SessionID 变化，或者数据库有变动时，UI 会自动刷新
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

    val completedExercises: StateFlow<List<String>> = setsToday.map { sets ->
        sets.map { it.exerciseName }.distinct()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun startNewSession() {
        val now = System.currentTimeMillis()
        _currentDate.value = dateFormatter.format(Date(now))
        _currentSessionId.value = "Session_$now"
    }

    fun addSet(exerciseId: String, weight: Double, reps: Int) {
        val now = System.currentTimeMillis()
        val dateStr = dateFormatter.format(Date(now))
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
            
            // 每次保存后，确保 currentDate 也是最新的（防止跨天训练）
            _currentDate.value = dateStr

            // 触发异步同步
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(Data.Builder().putString("SYNC_DATE", dateStr).build())
                .build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }

    suspend fun hasCompletedExercisesOnDate(dateStr: String): Boolean {
        return dao.getSetsByDate(dateStr).isNotEmpty()
    }

    /**
     * 检查某一天是否完成了计划中的所有动作及其目标组数
     */
    suspend fun isDayFullyCompleted(dateStr: String, routine: List<com.fitness.model.PlannedExercise>): Boolean {
        if (routine.isEmpty()) return false
        val setsOnDate = dao.getSetsByDate(dateStr)
        return routine.all { planned ->
            val completedCount = setsOnDate.count { it.exerciseName == planned.id }
            completedCount >= planned.targetSets
        }
    }
}
