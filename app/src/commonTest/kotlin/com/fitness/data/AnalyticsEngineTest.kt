package com.fitness.data

import com.fitness.model.ExerciseSet
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsEngineTest {
    @Test
    fun testCalculate1RM() {
        // Epley formula: Weight * (1 + 0.0333 * Reps)
        val set = ExerciseSet(reps = 10, weight = 100.0, date = "2024-03-10", sessionId = "s1", exerciseName = "benchpress", timestamp = 0L, timeStr = "10:00")
        val estimated1RM = AnalyticsEngine.calculate1RM(set)
        
        // 100 * (1 + 0.0333 * 10) = 100 * 1.333 = 133.3
        assertEquals(133.3, estimated1RM, 0.1)
    }

    @Test
    fun testCalculateVolumePerMuscleGroup() {
        val sets = listOf(
            ExerciseSet(reps = 10, weight = 100.0, date = "2024-03-10", sessionId = "s1", exerciseName = "benchpress", timestamp = 0L, timeStr = "10:00"),
            ExerciseSet(reps = 5, weight = 100.0, date = "2024-03-10", sessionId = "s1", exerciseName = "pushup", timestamp = 0L, timeStr = "10:05"), // Bodyweight, shouldn't add to absolute tonnage
            ExerciseSet(reps = 10, weight = 50.0, date = "2024-03-10", sessionId = "s1", exerciseName = "squat", timestamp = 0L, timeStr = "10:10")
        )
        
        val volumeMap = AnalyticsEngine.calculateVolumePerMuscleGroup(sets)
        
        // benchpress is chest (10 * 100 = 1000)
        // squat is legs (10 * 50 = 500)
        assertEquals(1000.0, volumeMap["cat_chest"])
        assertEquals(500.0, volumeMap["cat_legs"])
    }
}
