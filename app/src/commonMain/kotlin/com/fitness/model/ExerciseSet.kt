package com.fitness.model

import kotlinx.serialization.Serializable

/**
 * Platform-independent representation of a training set.
 */
@Serializable
data class ExerciseSet(
    val id: Long = 0,
    val date: String,
    val sessionId: String,
    val exerciseName: String,
    val reps: Int,
    val weight: Double,
    val timestamp: Long,
    val timeStr: String,
    val remoteId: String = "",
    val isDeleted: Boolean = false
)
