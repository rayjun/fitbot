package com.fitness.model

import androidx.annotation.StringRes

data class Exercise(
    val id: String,
    @StringRes val nameRes: Int,
    val gifResPath: String,
    @StringRes val descriptionRes: Int,
    @StringRes val targetMuscleRes: Int,
    @StringRes val categoryRes: Int,
    val isBodyweight: Boolean = false // 是否为自重训练
)
