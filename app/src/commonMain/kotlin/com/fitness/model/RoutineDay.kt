package com.fitness.model

import kotlinx.serialization.Serializable

@Serializable
data class PlannedExercise(
    val id: String,
    val targetSets: Int = 3
)

@Serializable
data class RoutineDay(
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val isRest: Boolean,
    val exercises: List<PlannedExercise>
)
