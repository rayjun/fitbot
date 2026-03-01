package com.fitness.data

import com.fitness.R
import com.fitness.model.Exercise

object ExerciseProvider {
    val exercises = listOf(
        Exercise(
            id = "benchpress",
            nameRes = R.string.ex_benchpress_name,
            gifResPath = "exercises/benchpress.gif",
            descriptionRes = R.string.ex_benchpress_desc,
            targetMuscleRes = R.string.muscle_chest,
            categoryRes = R.string.cat_chest
        ),
        Exercise(
            id = "pushup",
            nameRes = R.string.ex_pushup_name,
            gifResPath = "exercises/pushup.gif",
            descriptionRes = R.string.ex_pushup_desc,
            targetMuscleRes = R.string.muscle_chest,
            categoryRes = R.string.cat_chest
        ),
        Exercise(
            id = "incline_press",
            nameRes = R.string.ex_incline_press_name,
            gifResPath = "exercises/incline_press.gif",
            descriptionRes = R.string.ex_incline_press_desc,
            targetMuscleRes = R.string.muscle_upper_chest,
            categoryRes = R.string.cat_chest
        ),
        Exercise(
            id = "pullup_v2",
            nameRes = R.string.ex_pullup_name,
            gifResPath = "exercises/pullup.gif",
            descriptionRes = R.string.ex_pullup_desc,
            targetMuscleRes = R.string.muscle_lats,
            categoryRes = R.string.cat_back
        ),
        Exercise(
            id = "row",
            nameRes = R.string.ex_row_name,
            gifResPath = "exercises/row.gif",
            descriptionRes = R.string.ex_row_desc,
            targetMuscleRes = R.string.muscle_mid_back,
            categoryRes = R.string.cat_back
        ),
        Exercise(
            id = "squat",
            nameRes = R.string.ex_squat_name,
            gifResPath = "exercises/squat.gif",
            descriptionRes = R.string.ex_squat_desc,
            targetMuscleRes = R.string.muscle_quads,
            categoryRes = R.string.cat_legs
        ),
        Exercise(
            id = "lunge",
            nameRes = R.string.ex_lunge_name,
            gifResPath = "exercises/lunge.gif",
            descriptionRes = R.string.ex_lunge_desc,
            targetMuscleRes = R.string.muscle_glutes_legs,
            categoryRes = R.string.cat_legs
        ),
        Exercise(
            id = "plank",
            nameRes = R.string.ex_plank_name,
            gifResPath = "exercises/plank.gif",
            descriptionRes = R.string.ex_plank_desc,
            targetMuscleRes = R.string.muscle_abs,
            categoryRes = R.string.cat_core
        ),
        Exercise(
            id = "burpee",
            nameRes = R.string.ex_burpee_name,
            gifResPath = "exercises/burpee.gif",
            descriptionRes = R.string.ex_burpee_desc,
            targetMuscleRes = R.string.muscle_full_body,
            categoryRes = R.string.cat_full_body
        )
    )
    
    val categories = listOf(R.string.category_all) + exercises.map { it.categoryRes }.distinct()
}
