package com.fitness.model

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ExerciseSetTest {
    @Test
    fun testSetRecordHasIsDeletedDefaultFalse() {
        val record = SetRecord(reps = 10, weight = 50.0, time = "10:00", remoteId = "123")
        assertFalse(record.isDeleted)
    }

    @Test
    fun testExerciseSetHasIsDeletedDefaultFalse() {
        val set = ExerciseSet(
            id = 1L,
            date = "2024-03-10",
            sessionId = "s1",
            exerciseName = "squat",
            reps = 10,
            weight = 100.0,
            timestamp = 0L,
            timeStr = "12:00"
        )
        assertFalse(set.isDeleted)
    }
}
