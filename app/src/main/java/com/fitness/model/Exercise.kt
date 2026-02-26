package com.fitness.model

data class Exercise(
    val id: String,
    val name: String,
    val gifResPath: String, // 例如: exercises/squat.gif
    val description: String,
    val targetMuscle: String
)
