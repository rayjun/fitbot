package com.fitness.data

import com.fitness.model.ExerciseSet

object AnalyticsEngine {
    /**
     * Calculates Estimated 1 Rep Max using the Epley formula: 1RM = w * (1 + r/30)
     */
    fun calculate1RM(set: ExerciseSet): Double {
        if (set.reps <= 0 || set.weight <= 0) return 0.0
        return set.weight * (1.0 + (set.reps * 0.0333))
    }

    /**
     * Calculates total tonnage (Weight * Reps) for each muscle group.
     * Ignores bodyweight exercises.
     */
    fun calculateVolumePerMuscleGroup(sets: List<ExerciseSet>): Map<String, Double> {
        val volumeMap = mutableMapOf<String, Double>()
        
        sets.forEach { set ->
            val exercise = ExerciseProvider.exercises.find { it.id == set.exerciseName }
            // Filter out bodyweight and cardio exercises from total tonnage
            if (exercise != null && !exercise.isBodyweight && exercise.categoryKey != "cat_cardio" && !set.isDeleted) {
                val volume = set.weight * set.reps
                volumeMap[exercise.categoryKey] = (volumeMap[exercise.categoryKey] ?: 0.0) + volume
            }
        }
        
        return volumeMap
    }
}
