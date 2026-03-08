package com.fitness.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 对应数据库中的每一组训练记录。
 * 扁平化存储，方便查询和增量同步。
 */
@Entity(tableName = "exercise_sets")
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // 格式: YYYY-MM-DD
    val sessionId: String,     // 格式: 胸部训练_0900
    val exerciseName: String,  // 动作名称: 杠铃卧推
    val reps: Int,
    val weight: Double,
    val timestamp: Long,       // 用于记录和排序
    val timeStr: String,       // 格式: 09:05，方便序列化
    val remoteId: String = "" // 用于跨设备唯一标识
)

fun SetEntity.toModel() = com.fitness.model.ExerciseSet(
    id = id,
    date = date,
    sessionId = sessionId,
    exerciseName = exerciseName,
    reps = reps,
    weight = weight,
    timestamp = timestamp,
    timeStr = timeStr,
    remoteId = remoteId
)
