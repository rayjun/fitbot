package com.fitness.model

import kotlinx.serialization.Serializable

/**
 * Platform-independent representation of a workout plan.
 */
@Serializable
data class WorkoutPlan(
    val id: Long = 0,
    val name: String,
    val exercisesJson: String,
    val createdAt: Long,
    val isCurrent: Boolean = false
)
