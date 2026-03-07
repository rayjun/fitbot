package com.fitness.model

data class Exercise(
    val id: String,
    val nameRes: Int,
    val gifResPath: String,
    val descriptionRes: Int,
    val targetMuscleRes: Int,
    val categoryRes: Int,
    val isBodyweight: Boolean = false // 是否为自重训练
)
