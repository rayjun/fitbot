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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkoutViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.exerciseDao()

    private var _currentSessionId = "Session_${System.currentTimeMillis()}"
    private val _setsInSession = MutableStateFlow<List<SetEntity>>(emptyList())
    val setsInSession: StateFlow<List<SetEntity>> = _setsInSession

    // 被完成的动作名称集合
    val completedExercises = _setsInSession.map { sets ->
        sets.map { it.exerciseName }.distinct()
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        refreshSets()
    }

    fun startNewSession() {
        _currentSessionId = "Session_${System.currentTimeMillis()}"
        _setsInSession.value = emptyList()
    }

    fun addSet(exerciseName: String, weight: Double, reps: Int) {
        val now = System.currentTimeMillis()
        val set = SetEntity(
            date = dateFormatter.format(Date(now)),
            sessionId = _currentSessionId,
            exerciseName = exerciseName,
            reps = reps,
            weight = weight,
            timestamp = now,
            timeStr = timeFormatter.format(Date(now))
        )

        viewModelScope.launch {
            dao.insertSet(set)
            refreshSets()
        }
    }

    fun refreshSets() {
        viewModelScope.launch {
            _setsInSession.value = dao.getSetsByDate(dateFormatter.format(Date()))
                .filter { it.sessionId == _currentSessionId }
        }
    }

    fun finishWorkout() {
        // 触发同步任务
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(Data.Builder().putString("SYNC_DATE", dateFormatter.format(Date())).build())
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
        // 完成后不一定重置，让 UI 处理跳转
    }
}
