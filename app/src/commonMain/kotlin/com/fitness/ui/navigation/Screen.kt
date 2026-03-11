package com.fitness.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val labelKey: String? = null, val icon: ImageVector? = null) {
    object Library : Screen("library", "nav_library", Icons.Default.FitnessCenter)
    object Plans : Screen("plans", "nav_plans", Icons.Default.ListAlt)
    object Profile : Screen("profile", "nav_profile", Icons.Default.Person)
    object Workout : Screen("workout/{exerciseId}/{date}") {
        fun createRoute(exerciseId: String, date: String) = "workout/$exerciseId/$date"
    }
    object ExerciseDetail : Screen("exercise_detail/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise_detail/$exerciseId"
    }
    object PlanSession : Screen("plan_session/{dayOfWeek}") {
        fun createRoute(dayOfWeek: Int) = "plan_session/$dayOfWeek"
    }
    object DayDetails : Screen("day_details/{date}") {
        fun createRoute(date: String) = "day_details/$date"
    }
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
}
