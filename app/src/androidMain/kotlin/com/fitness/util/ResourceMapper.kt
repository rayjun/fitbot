package com.fitness.util

import android.content.Context
import com.fitness.data.local.SetEntity
import com.fitness.data.local.PlanEntity
import com.fitness.model.ExerciseSet
import com.fitness.model.WorkoutPlan

/**
 * Maps a string key (like "ex_benchpress_name") to its corresponding Android resource ID.
 * This is used to maintain compatibility after moving models to commonMain where R.string is not available.
 */
fun String.toResId(context: Context): Int {
    return context.resources.getIdentifier(this, "string", context.packageName)
}

fun SetEntity.toModel() = ExerciseSet(
    id = id,
    date = date,
    sessionId = sessionId,
    exerciseName = exerciseName,
    reps = reps,
    weight = weight,
    timestamp = timestamp,
    timeStr = timeStr,
    remoteId = remoteId
)

fun PlanEntity.toModel() = WorkoutPlan(
    id = id,
    name = name,
    exercisesJson = exercisesJson,
    createdAt = createdAt,
    isCurrent = isCurrent
)
