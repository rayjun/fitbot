package com.fitness.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HeatMapData(val date: String, val count: Int)

class ProfileViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.exerciseDao()

    private val _heatmapData = MutableStateFlow<Map<String, Int>>(emptyList<HeatMapData>().associate { it.date to it.count })
    val heatmapData: StateFlow<Map<String, Int>> = _heatmapData

    init {
        loadHeatmap()
    }

    private fun loadHeatmap() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -90)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        viewModelScope.launch {
            val sets = dao.getSetsSinceDate(startDate)
            _heatmapData.value = sets.groupBy { it.date }.mapValues { it.value.size }
        }
    }
}
