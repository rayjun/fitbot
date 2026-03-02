package com.fitness.data

import com.fitness.R
import com.fitness.model.Exercise

object ExerciseProvider {
    val exercises = listOf(
        // 胸部 (Chest)
        Exercise(
            id = "benchpress",
            nameRes = R.string.ex_benchpress_name,
            gifResPath = "exercises/benchpress.gif",
            descriptionRes = R.string.ex_benchpress_desc,
            targetMuscleRes = R.string.muscle_chest,
            categoryRes = R.string.cat_chest,
            isBodyweight = false
        ),
        Exercise(
            id = "pushup",
            nameRes = R.string.ex_pushup_name,
            gifResPath = "exercises/pushup.gif",
            descriptionRes = R.string.ex_pushup_desc,
            targetMuscleRes = R.string.muscle_chest,
            categoryRes = R.string.cat_chest,
            isBodyweight = true
        ),
        Exercise(
            id = "incline_press",
            nameRes = R.string.ex_incline_press_name,
            gifResPath = "exercises/incline_press.gif",
            descriptionRes = R.string.ex_incline_press_desc,
            targetMuscleRes = R.string.muscle_upper_chest,
            categoryRes = R.string.cat_chest,
            isBodyweight = false
        ),
        
        // 肩部 (Shoulders)
        Exercise(
            id = "overhead_press",
            nameRes = R.string.ex_overhead_press_name,
            gifResPath = "exercises/overhead_press.gif",
            descriptionRes = R.string.ex_overhead_press_desc,
            targetMuscleRes = R.string.muscle_shoulders,
            categoryRes = R.string.cat_shoulders,
            isBodyweight = false
        ),
        Exercise(
            id = "lateral_raise",
            nameRes = R.string.ex_lateral_raise_name,
            gifResPath = "exercises/lateral_raise.gif",
            descriptionRes = R.string.ex_lateral_raise_desc,
            targetMuscleRes = R.string.muscle_shoulders,
            categoryRes = R.string.cat_shoulders,
            isBodyweight = false
        ),

        // 背部 (Back)
        Exercise(
            id = "pullup_v2",
            nameRes = R.string.ex_pullup_name,
            gifResPath = "exercises/pullup.gif",
            descriptionRes = R.string.ex_pullup_desc,
            targetMuscleRes = R.string.muscle_lats,
            categoryRes = R.string.cat_back,
            isBodyweight = true
        ),
        Exercise(
            id = "row",
            nameRes = R.string.ex_row_name,
            gifResPath = "exercises/row.gif",
            descriptionRes = R.string.ex_row_desc,
            targetMuscleRes = R.string.muscle_mid_back,
            categoryRes = R.string.cat_back,
            isBodyweight = false
        ),
        Exercise(
            id = "deadlift",
            nameRes = R.string.ex_deadlift_name,
            gifResPath = "exercises/deadlift.gif",
            descriptionRes = R.string.ex_deadlift_desc,
            targetMuscleRes = R.string.muscle_lower_back,
            categoryRes = R.string.cat_back,
            isBodyweight = false
        ),

        // 手臂 (Arms)
        Exercise(
            id = "bicep_curl",
            nameRes = R.string.ex_bicep_curl_name,
            gifResPath = "exercises/bicep_curl.gif",
            descriptionRes = R.string.ex_bicep_curl_desc,
            targetMuscleRes = R.string.muscle_biceps,
            categoryRes = R.string.cat_arms,
            isBodyweight = false
        ),
        Exercise(
            id = "tricep_dips",
            nameRes = R.string.ex_tricep_dips_name,
            gifResPath = "exercises/tricep_dips.gif",
            descriptionRes = R.string.ex_tricep_dips_desc,
            targetMuscleRes = R.string.muscle_triceps,
            categoryRes = R.string.cat_arms,
            isBodyweight = true
        ),

        // 腿部 (Legs)
        Exercise(
            id = "squat",
            nameRes = R.string.ex_squat_name,
            gifResPath = "exercises/squat.gif",
            descriptionRes = R.string.ex_squat_desc,
            targetMuscleRes = R.string.muscle_quads,
            categoryRes = R.string.cat_legs,
            isBodyweight = false
        ),
        Exercise(
            id = "lunge",
            nameRes = R.string.ex_lunge_name,
            gifResPath = "exercises/lunge.gif",
            descriptionRes = R.string.ex_lunge_desc,
            targetMuscleRes = R.string.muscle_glutes_legs,
            categoryRes = R.string.cat_legs,
            isBodyweight = false
        ),
        Exercise(
            id = "calf_raise",
            nameRes = R.string.ex_calf_raise_name,
            gifResPath = "exercises/calf_raise.gif",
            descriptionRes = R.string.ex_calf_raise_desc,
            targetMuscleRes = R.string.muscle_calves,
            categoryRes = R.string.cat_legs,
            isBodyweight = false
        ),

        // 核心 (Core)
        Exercise(
            id = "situp",
            nameRes = R.string.ex_situp_name,
            gifResPath = "exercises/situp.gif",
            descriptionRes = R.string.ex_situp_desc,
            targetMuscleRes = R.string.muscle_abs,
            categoryRes = R.string.cat_core,
            isBodyweight = true
        ),
        Exercise(
            id = "crunches",
            nameRes = R.string.ex_crunches_name,
            gifResPath = "exercises/crunches.gif",
            descriptionRes = R.string.ex_crunches_desc,
            targetMuscleRes = R.string.muscle_abs,
            categoryRes = R.string.cat_core,
            isBodyweight = true
        ),
        Exercise(
            id = "russian_twist",
            nameRes = R.string.ex_russian_twist_name,
            gifResPath = "exercises/russian_twist.gif",
            descriptionRes = R.string.ex_russian_twist_desc,
            targetMuscleRes = R.string.muscle_abs,
            categoryRes = R.string.cat_core,
            isBodyweight = true
        ),
        Exercise(
            id = "plank",
            nameRes = R.string.ex_plank_name,
            gifResPath = "exercises/plank.gif",
            descriptionRes = R.string.ex_plank_desc,
            targetMuscleRes = R.string.muscle_abs,
            categoryRes = R.string.cat_core,
            isBodyweight = true
        ),

        // 全身 (Full Body)
        Exercise(
            id = "burpee",
            nameRes = R.string.ex_burpee_name,
            gifResPath = "exercises/burpee.gif",
            descriptionRes = R.string.ex_burpee_desc,
            targetMuscleRes = R.string.muscle_full_body,
            categoryRes = R.string.cat_full_body,
            isBodyweight = true
        )
    )
    
    val categories = listOf(R.string.category_all) + exercises.map { it.categoryRes }.distinct()
}
