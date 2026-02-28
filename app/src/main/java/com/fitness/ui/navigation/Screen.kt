package com.fitness.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Library : Screen("library", "Library", Icons.Default.FitnessCenter)
    object Plans : Screen("plans", "Plans", Icons.Default.ListAlt)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Workout : Screen("workout/{exerciseName}") {
        fun createRoute(exerciseName: String) = "workout/$exerciseName"
    }
    object PlanSession : Screen("plan_session/{planId}") {
        fun createRoute(planId: Int) = "plan_session/$planId"
    }
}
