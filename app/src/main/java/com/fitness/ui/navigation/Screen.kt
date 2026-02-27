package com.fitness.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Library : Screen("library", "健身", Icons.Default.FitnessCenter)
    object Plans : Screen("plans", "计划", Icons.Default.ListAlt)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    object Workout : Screen("workout/{exerciseName}") {
        fun createRoute(exerciseName: String) = "workout/$exerciseName"
    }
}
