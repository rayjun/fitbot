package com.fitness.util

import androidx.compose.runtime.Composable

private val stringMap = mapOf(
    "app_name" to "FitBot",
    "nav_library" to "Library",
    "nav_plans" to "Plans",
    "nav_profile" to "Profile",
    "category_all" to "All",
    "cat_chest" to "Chest",
    "cat_back" to "Back",
    "cat_legs" to "Legs",
    "cat_shoulders" to "Shoulders",
    "cat_arms" to "Arms",
    "cat_core" to "Core",
    "cat_full_body" to "Full Body",
    "ex_benchpress_name" to "Bench Press",
    "ex_pushup_name" to "Push-up",
    "ex_incline_press_name" to "Incline Bench Press",
    "ex_pullup_name" to "Pull-up",
    "ex_row_name" to "Barbell Row",
    "ex_deadlift_name" to "Deadlift",
    "ex_squat_name" to "Squat",
    "ex_lunge_name" to "Lunge",
    "ex_calf_raise_name" to "Calf Raise",
    "ex_overhead_press_name" to "Overhead Press",
    "ex_lateral_raise_name" to "Lateral Raise",
    "ex_bicep_curl_name" to "Bicep Curl",
    "ex_tricep_dips_name" to "Tricep Dips",
    "ex_situp_name" to "Sit-up",
    "ex_crunches_name" to "Crunches",
    "ex_russian_twist_name" to "Russian Twist",
    "ex_plank_name" to "Plank",
    "ex_burpee_name" to "Burpee",
    "muscle_chest" to "Pectorals",
    "muscle_lats" to "Lats",
    "muscle_quads" to "Quads",
    "muscle_shoulders" to "Deltoids",
    "muscle_biceps" to "Biceps",
    "muscle_triceps" to "Triceps",
    "muscle_abs" to "Abs",
    "heatmap_title" to "Workout Heatmap",
    "login_drive" to "Login to Google Drive",
    "logout" to "Logout"
)

@Composable
actual fun getString(key: String): String {
    return stringMap[key] ?: key
}
