package com.fitness.model

data class PlannedExercise(
    val id: String,
    val targetSets: Int = 3
)

data class RoutineDay(
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val isRest: Boolean,
    val exercises: List<PlannedExercise>
)
