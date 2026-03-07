package com.fitness.data

import com.fitness.model.RoutineDay
import com.fitness.model.PlannedExercise
import com.fitness.model.ExerciseSet
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    // Plans logic
    fun getCurrentRoutine(): Flow<List<RoutineDay>>
    suspend fun updateRoutineDay(dayOfWeek: Int, isRest: Boolean, exercises: List<PlannedExercise>)
    
    // History / Heatmap logic
    fun getHeatmapData(): Flow<Map<String, Int>>
    fun getSetsByDate(date: String): Flow<List<ExerciseSet>>
    suspend fun addExerciseSet(set: ExerciseSet)
}
