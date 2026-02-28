package com.fitness.model

data class RoutineDay(
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val isRest: Boolean,
    val exercises: List<String> // List of exercise IDs
)
