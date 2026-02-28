package com.fitness.model

import com.google.gson.annotations.SerializedName

/**
 * 代表一天的所有训练数据，用于 JSON 序列化同步到 Google Drive
 */
data class TrainingDay(
    @SerializedName("date") val date: String,
    @SerializedName("sessions") val sessions: List<TrainingSession>
)

data class TrainingSession(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("exercises") val exercises: List<ExerciseRecord>
)

data class ExerciseRecord(
    @SerializedName("name") val name: String,
    @SerializedName("sets") val sets: List<SetRecord>
)

data class SetRecord(
    @SerializedName("reps") val reps: Int,
    @SerializedName("weight") val weight: Double,
    @SerializedName("time") val time: String,
    @SerializedName("remoteId") val remoteId: String
)
