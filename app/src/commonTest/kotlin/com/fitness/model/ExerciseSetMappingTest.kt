package com.fitness.model

import com.fitness.data.local.SetEntity
import com.fitness.data.local.toModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExerciseSetMappingTest {
    @Test
    fun testEntityToModelMapping() {
        val entity = SetEntity(
            id = 100L,
            date = "2024-03-13",
            sessionId = "session_1",
            exerciseName = "running",
            reps = 0,
            weight = 0.0,
            distance = 5.5,
            duration = 45,
            timestamp = 123456789L,
            timeStr = "08:00",
            remoteId = "remote_123",
            isDeleted = false
        )
        
        val model = entity.toModel()
        
        assertEquals(entity.id, model.id)
        assertEquals(entity.distance, model.distance)
        assertEquals(entity.duration, model.duration)
        assertEquals(5.5, model.distance)
        assertEquals(45, model.duration)
    }

    @Test
    fun testModelToEntityCompatibility() {
        val model = ExerciseSet(
            id = 200L,
            date = "2024-03-13",
            sessionId = "session_2",
            exerciseName = "benchpress",
            reps = 10,
            weight = 100.0,
            distance = null, // Default for strength
            duration = null, // Default for strength
            timestamp = 987654321L,
            timeStr = "18:00"
        )
        
        assertNull(model.distance)
        assertNull(model.duration)
        assertEquals(100.0, model.weight)
    }
}
