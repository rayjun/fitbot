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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkoutViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.exerciseDao()

    private var _currentSessionId = "Session_${System.currentTimeMillis()}"
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 使用 FlatMapLatest 动态观察当前日期的数据库流，并按会话 ID 过滤
    val setsInSession: StateFlow<List<SetEntity>> = dao.getSetsByDateFlow(dateFormatter.format(Date()))
        .map { list -> list.filter { it.sessionId == _currentSessionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 衍生状态：已完成的动作 ID 集合
    val completedExercises: StateFlow<List<String>> = setsInSession.map { sets ->
        sets.map { it.exerciseName }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startNewSession() {
        _currentSessionId = "Session_${System.currentTimeMillis()}"
        // Flow 会自动刷新，无需手动清空 _setsInSession.value
    }

    fun addSet(exerciseId: String, weight: Double, reps: Int) {
        val now = System.currentTimeMillis()
        val set = SetEntity(
            date = dateFormatter.format(Date(now)),
            sessionId = _currentSessionId,
            exerciseName = exerciseId, // 存储 ID
            reps = reps,
            weight = weight,
            timestamp = now,
            timeStr = timeFormatter.format(Date(now))
        )

        viewModelScope.launch {
            // 1. 立即保存到本地数据库
            dao.insertSet(set)
            
            // 2. 自动触发后台同步任务（WorkManager 会自动处理合并与网络检查）
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(Data.Builder().putString("SYNC_DATE", dateFormatter.format(Date(now))).build())
                .build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }

    fun refreshSets() {
        // 由于使用了 Flow，不需要手动 refresh
    }

    suspend fun hasCompletedExercisesOnDate(dateStr: String): Boolean {
        return dao.getSetsByDate(dateStr).isNotEmpty()
    }
}
