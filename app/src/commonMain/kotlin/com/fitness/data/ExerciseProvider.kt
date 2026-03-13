package com.fitness.data

import com.fitness.model.Exercise

object ExerciseProvider {
    val exercises = listOf(
        // 胸部 (Chest)
        Exercise(
            id = "benchpress",
            nameKey = "ex_benchpress_name",
            gifResPath = "exercises/benchpress.gif",
            descriptionKey = "ex_benchpress_desc",
            targetMuscleKey = "muscle_chest",
            categoryKey = "cat_chest",
            isBodyweight = false
        ),
        Exercise(
            id = "pushup",
            nameKey = "ex_pushup_name",
            gifResPath = "exercises/pushup.gif",
            descriptionKey = "ex_pushup_desc",
            targetMuscleKey = "muscle_chest",
            categoryKey = "cat_chest",
            isBodyweight = true
        ),
        Exercise(
            id = "incline_press",
            nameKey = "ex_incline_press_name",
            gifResPath = "exercises/incline_press.gif",
            descriptionKey = "ex_incline_press_desc",
            targetMuscleKey = "muscle_upper_chest",
            categoryKey = "cat_chest",
            isBodyweight = false
        ),
        Exercise(
            id = "dumbbell_fly",
            nameKey = "ex_dumbbell_fly_name",
            gifResPath = "exercises/dumbbell_fly.gif",
            descriptionKey = "ex_dumbbell_fly_desc",
            targetMuscleKey = "muscle_chest",
            categoryKey = "cat_chest",
            isBodyweight = false
        ),
        Exercise(
            id = "cable_crossover",
            nameKey = "ex_cable_crossover_name",
            gifResPath = "exercises/cable_crossover.gif",
            descriptionKey = "ex_cable_crossover_desc",
            targetMuscleKey = "muscle_chest",
            categoryKey = "cat_chest",
            isBodyweight = false
        ),
        
        // 肩部 (Shoulders)
        Exercise(
            id = "overhead_press",
            nameKey = "ex_overhead_press_name",
            gifResPath = "exercises/overhead_press.gif",
            descriptionKey = "ex_overhead_press_desc",
            targetMuscleKey = "muscle_shoulders",
            categoryKey = "cat_shoulders",
            isBodyweight = false
        ),
        Exercise(
            id = "lateral_raise",
            nameKey = "ex_lateral_raise_name",
            gifResPath = "exercises/lateral_raise.gif",
            descriptionKey = "ex_lateral_raise_desc",
            targetMuscleKey = "muscle_shoulders",
            categoryKey = "cat_shoulders",
            isBodyweight = false
        ),
        Exercise(
            id = "front_raise",
            nameKey = "ex_front_raise_name",
            gifResPath = "exercises/front_raise.gif",
            descriptionKey = "ex_front_raise_desc",
            targetMuscleKey = "muscle_front_delts",
            categoryKey = "cat_shoulders",
            isBodyweight = false
        ),

        // 背部 (Back)
        Exercise(
            id = "pullup_v2",
            nameKey = "ex_pullup_name",
            gifResPath = "exercises/pullup.gif",
            descriptionKey = "ex_pullup_desc",
            targetMuscleKey = "muscle_lats",
            categoryKey = "cat_back",
            isBodyweight = true
        ),
        Exercise(
            id = "lat_pulldown",
            nameKey = "ex_lat_pulldown_name",
            gifResPath = "exercises/lat_pulldown.gif",
            descriptionKey = "ex_lat_pulldown_desc",
            targetMuscleKey = "muscle_lats",
            categoryKey = "cat_back",
            isBodyweight = false
        ),
        Exercise(
            id = "row",
            nameKey = "ex_row_name",
            gifResPath = "exercises/row.gif",
            descriptionKey = "ex_row_desc",
            targetMuscleKey = "muscle_mid_back",
            categoryKey = "cat_back",
            isBodyweight = false
        ),
        Exercise(
            id = "face_pull",
            nameKey = "ex_face_pull_name",
            gifResPath = "exercises/face_pull.gif",
            descriptionKey = "ex_face_pull_desc",
            targetMuscleKey = "muscle_rear_delts",
            categoryKey = "cat_back",
            isBodyweight = false
        ),
        Exercise(
            id = "deadlift",
            nameKey = "ex_deadlift_name",
            gifResPath = "exercises/deadlift.gif",
            descriptionKey = "ex_deadlift_desc",
            targetMuscleKey = "muscle_lower_back",
            categoryKey = "cat_back",
            isBodyweight = false
        ),

        // 手臂 (Arms)
        Exercise(
            id = "bicep_curl",
            nameKey = "ex_bicep_curl_name",
            gifResPath = "exercises/bicep_curl.gif",
            descriptionKey = "ex_bicep_curl_desc",
            targetMuscleKey = "muscle_biceps",
            categoryKey = "cat_arms",
            isBodyweight = false
        ),
        Exercise(
            id = "hammer_curl",
            nameKey = "ex_hammer_curl_name",
            gifResPath = "exercises/hammer_curl.gif",
            descriptionKey = "ex_hammer_curl_desc",
            targetMuscleKey = "muscle_biceps",
            categoryKey = "cat_arms",
            isBodyweight = false
        ),
        Exercise(
            id = "tricep_dips",
            nameKey = "ex_tricep_dips_name",
            gifResPath = "exercises/tricep_dips.gif",
            descriptionKey = "ex_tricep_dips_desc",
            targetMuscleKey = "muscle_triceps",
            categoryKey = "cat_arms",
            isBodyweight = true
        ),
        Exercise(
            id = "triceps_pushdown",
            nameKey = "ex_triceps_pushdown_name",
            gifResPath = "exercises/triceps_pushdown.gif",
            descriptionKey = "ex_triceps_pushdown_desc",
            targetMuscleKey = "muscle_triceps",
            categoryKey = "cat_arms",
            isBodyweight = false
        ),

        // 腿部 (Legs)
        Exercise(
            id = "squat",
            nameKey = "ex_squat_name",
            gifResPath = "exercises/squat.gif",
            descriptionKey = "ex_squat_desc",
            targetMuscleKey = "muscle_quads",
            categoryKey = "cat_legs",
            isBodyweight = false
        ),
        Exercise(
            id = "leg_press",
            nameKey = "ex_leg_press_name",
            gifResPath = "exercises/leg_press.gif",
            descriptionKey = "ex_leg_press_desc",
            targetMuscleKey = "muscle_quads",
            categoryKey = "cat_legs",
            isBodyweight = false
        ),
        Exercise(
            id = "leg_extension",
            nameKey = "ex_leg_extension_name",
            gifResPath = "exercises/leg_extension.gif",
            descriptionKey = "ex_leg_extension_desc",
            targetMuscleKey = "muscle_quads",
            categoryKey = "cat_legs",
            isBodyweight = false
        ),
        Exercise(
            id = "leg_curl",
            nameKey = "ex_leg_curl_name",
            gifResPath = "exercises/leg_curl.gif",
            descriptionKey = "ex_leg_curl_desc",
            targetMuscleKey = "muscle_hamstrings",
            categoryKey = "cat_legs",
            isBodyweight = false
        ),
        Exercise(
            id = "romanian_deadlift",
            nameKey = "ex_romanian_deadlift_name",
            gifResPath = "exercises/romanian_deadlift.gif",
            descriptionKey = "ex_romanian_deadlift_desc",
            targetMuscleKey = "muscle_hamstrings",
            categoryKey = "cat_legs",
            isBodyweight = false
        ),
        Exercise(
            id = "lunge",
            nameKey = "ex_lunge_name",
            gifResPath = "exercises/lunge.gif",
            descriptionKey = "ex_lunge_desc",
            targetMuscleKey = "muscle_glutes_legs",
            categoryKey = "cat_legs",
            isBodyweight = false
        ),
        Exercise(
            id = "calf_raise",
            nameKey = "ex_calf_raise_name",
            gifResPath = "exercises/calf_raise.gif",
            descriptionKey = "ex_calf_raise_desc",
            targetMuscleKey = "muscle_calves",
            categoryKey = "cat_legs",
            isBodyweight = false
        ),

        // 核心 (Core)
        Exercise(
            id = "situp",
            nameKey = "ex_situp_name",
            gifResPath = "exercises/situp.gif",
            descriptionKey = "ex_situp_desc",
            targetMuscleKey = "muscle_abs",
            categoryKey = "cat_core",
            isBodyweight = true
        ),
        Exercise(
            id = "hanging_leg_raise",
            nameKey = "ex_hanging_leg_raise_name",
            gifResPath = "exercises/hanging_leg_raise.gif",
            descriptionKey = "ex_hanging_leg_raise_desc",
            targetMuscleKey = "muscle_abs",
            categoryKey = "cat_core",
            isBodyweight = true
        ),
        Exercise(
            id = "crunches",
            nameKey = "ex_crunches_name",
            gifResPath = "exercises/crunches.gif",
            descriptionKey = "ex_crunches_desc",
            targetMuscleKey = "muscle_abs",
            categoryKey = "cat_core",
            isBodyweight = true
        ),
        Exercise(
            id = "russian_twist",
            nameKey = "ex_russian_twist_name",
            gifResPath = "exercises/russian_twist.gif",
            descriptionKey = "ex_russian_twist_desc",
            targetMuscleKey = "muscle_abs",
            categoryKey = "cat_core",
            isBodyweight = true
        ),
        Exercise(
            id = "plank",
            nameKey = "ex_plank_name",
            gifResPath = "exercises/plank.gif",
            descriptionKey = "ex_plank_desc",
            targetMuscleKey = "muscle_abs",
            categoryKey = "cat_core",
            isBodyweight = true
        ),

        // 全身 (Full Body)
        Exercise(
            id = "burpee",
            nameKey = "ex_burpee_name",
            gifResPath = "exercises/burpee.gif",
            descriptionKey = "ex_burpee_desc",
            targetMuscleKey = "muscle_full_body",
            categoryKey = "cat_full_body",
            isBodyweight = true
        ),

        // 有氧 (Cardio)
        Exercise(
            id = "running",
            nameKey = "ex_running_name",
            gifResPath = "exercises/running.gif",
            descriptionKey = "ex_running_desc",
            targetMuscleKey = "muscle_cardio",
            categoryKey = "cat_cardio",
            isBodyweight = true
        ),
        Exercise(
            id = "brisk_walking",
            nameKey = "ex_brisk_walking_name",
            gifResPath = "exercises/brisk_walking.gif",
            descriptionKey = "ex_brisk_walking_desc",
            targetMuscleKey = "muscle_cardio",
            categoryKey = "cat_cardio",
            isBodyweight = true
        ),
        Exercise(
            id = "cycling",
            nameKey = "ex_cycling_name",
            gifResPath = "exercises/cycling.gif",
            descriptionKey = "ex_cycling_desc",
            targetMuscleKey = "muscle_cardio",
            categoryKey = "cat_cardio",
            isBodyweight = true
        )
    )
    
    val categories = listOf("category_all") + exercises.map { it.categoryKey }.distinct()
}
