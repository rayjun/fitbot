package com.fitness.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val nameKey: String,
    val gifResPath: String,
    val descriptionKey: String,
    val targetMuscleKey: String,
    val categoryKey: String,
    val isBodyweight: Boolean = false // 是否为自重训练
)
