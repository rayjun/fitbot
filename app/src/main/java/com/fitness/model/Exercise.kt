package com.fitness.model

data class Exercise(
    val id: String,
    val name: String,
    val gifResPath: String,
    val description: String,
    val targetMuscle: String,
    val category: String
)
