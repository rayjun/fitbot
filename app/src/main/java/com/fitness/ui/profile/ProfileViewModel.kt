package com.fitness.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.local.AppDatabase
import com.fitness.data.local.SetEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*

class ProfileViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.exerciseDao()

    private val allSets: StateFlow<List<SetEntity>> = dao.getAllSetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 响应式的热力图数据
    val heatmapData: StateFlow<Map<String, Int>> = allSets.map { sets ->
        sets.groupBy { it.date }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
