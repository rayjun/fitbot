package com.fitness.model

import kotlinx.serialization.Serializable

/**
 * 代表一天的所有训练数据，用于 JSON 序列化同步到 Google Drive
 */
@Serializable
data class TrainingDay(
    val date: String,
    val sessions: List<TrainingSession>
)

@Serializable
data class TrainingSession(
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val exercises: List<ExerciseRecord>
)

@Serializable
data class ExerciseRecord(
    val name: String,
    val sets: List<SetRecord>
)

@Serializable
data class SetRecord(
    val reps: Int,
    val weight: Double,
    val time: String,
    val remoteId: String,
    val isDeleted: Boolean = false
)
