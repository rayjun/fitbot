package com.fitness.sync

import com.fitness.model.ExerciseRecord
import com.fitness.model.SetRecord
import com.fitness.model.TrainingDay
import com.fitness.model.TrainingSession
import com.fitness.model.mergeTrainingDays
import kotlin.test.Test
import kotlin.test.assertEquals

class MergeLogicTest {
    @Test
    fun testMergeTrainingDays() {
        val remoteSet = SetRecord(10, 50.0, "10:00", "remote-1")
        val localSet = SetRecord(12, 55.0, "10:05", "local-1")

        val remoteDay = TrainingDay(
            date = "2024-03-10",
            sessions = listOf(
                TrainingSession(
                    sessionId = "s1",
                    startTime = "10:00",
                    endTime = "10:00",
                    exercises = listOf(ExerciseRecord(name = "squat", sets = listOf(remoteSet)))
                )
            )
        )

        val localDay = TrainingDay(
            date = "2024-03-10",
            sessions = listOf(
                TrainingSession(
                    sessionId = "s1",
                    startTime = "10:00",
                    endTime = "10:05",
                    exercises = listOf(ExerciseRecord(name = "squat", sets = listOf(localSet)))
                )
            )
        )

        val mergedDay = mergeTrainingDays(localDay, remoteDay)

        assertEquals("2024-03-10", mergedDay.date)
        assertEquals(1, mergedDay.sessions.size)
        assertEquals(1, mergedDay.sessions[0].exercises.size)
        // Both sets should be preserved
        assertEquals(2, mergedDay.sessions[0].exercises[0].sets.size)
    }
}
