package com.fitness.data

import com.fitness.model.ExerciseSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AnalyticsEngineTest {
    @Test
    fun testCalculate1RM() {
        // Epley formula: Weight * (1 + 0.0333 * Reps)
        val set = ExerciseSet(reps = 10, weight = 100.0, date = "2024-03-10", sessionId = "s1", exerciseName = "benchpress", timestamp = 0L, timeStr = "10:00")
        val estimated1RM = AnalyticsEngine.calculate1RM(set)
        
        // 100 * (1 + 0.333) = 133.3
        assertEquals(133.3, estimated1RM, 0.1)
    }

    @Test
    fun testCalculateVolumePerMuscleGroupWithCardioIsolation() {
        val sets = listOf(
            // Strength (Chest) - 1000.0
            ExerciseSet(reps = 10, weight = 100.0, date = "2024-03-10", sessionId = "s1", exerciseName = "benchpress", timestamp = 0L, timeStr = "10:00"),
            
            // Bodyweight (Chest) - 0.0 tonnage
            ExerciseSet(reps = 5, weight = 0.0, date = "2024-03-10", sessionId = "s1", exerciseName = "pushup", timestamp = 0L, timeStr = "10:05"),
            
            // Strength (Legs) - 500.0
            ExerciseSet(reps = 10, weight = 50.0, date = "2024-03-10", sessionId = "s1", exerciseName = "squat", timestamp = 0L, timeStr = "10:10"),
            
            // Cardio (Running) - 5km / 30min. Should be ignored in volume calculation
            ExerciseSet(reps = 0, weight = 0.0, distance = 5.0, duration = 30, date = "2024-03-10", sessionId = "s1", exerciseName = "running", timestamp = 0L, timeStr = "10:15")
        )
        
        val volumeMap = AnalyticsEngine.calculateVolumePerMuscleGroup(sets)
        
        assertEquals(1000.0, volumeMap["cat_chest"])
        assertEquals(500.0, volumeMap["cat_legs"])
        
        // Verify cardio category is not present in tonnage charts
        assertFalse(volumeMap.containsKey("cat_cardio"), "Cardio exercises should not contribute to tonnage volume metrics.")
    }
}
