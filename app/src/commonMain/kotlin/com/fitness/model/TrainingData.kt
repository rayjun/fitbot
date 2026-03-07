package com.fitness.model

/**
 * 代表一天的所有训练数据，用于 JSON 序列化同步到 Google Drive
 */
data class TrainingDay(
    val date: String,
    val sessions: List<TrainingSession>
)

data class TrainingSession(
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val exercises: List<ExerciseRecord>
)

data class ExerciseRecord(
    val name: String,
    val sets: List<SetRecord>
)

data class SetRecord(
    val reps: Int,
    val weight: Double,
    val time: String,
    val remoteId: String
)
