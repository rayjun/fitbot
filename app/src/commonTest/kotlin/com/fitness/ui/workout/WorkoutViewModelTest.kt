package com.fitness.ui.workout

import com.fitness.data.FakeWorkoutRepository
import com.fitness.model.ExerciseSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private lateinit var viewModel: WorkoutViewModel
    private lateinit var repository: FakeWorkoutRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWorkoutRepository()
        viewModel = WorkoutViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSetDate() = runTest {
        val newDate = "2024-03-09"
        viewModel.setDate(newDate)
        assertEquals(newDate, viewModel.currentDate.value)
    }

    @Test
    fun testAddSet() = runTest {
        backgroundScope.launch { viewModel.setsToday.collect() }
        val date = "2024-03-09"
        viewModel.setDate(date)
        
        viewModel.addSet("benchpress", 80.0, 12)
        advanceUntilIdle()
        
        val sets = viewModel.setsToday.value
        assertEquals(1, sets.size)
        assertEquals("benchpress", sets[0].exerciseName)
        assertEquals(80.0, sets[0].weight)
        assertEquals(12, sets[0].reps)
        assertEquals(date, sets[0].date)
    }

    @Test
    fun testDeleteSetIsSoftDelete() = runTest {
        backgroundScope.launch { viewModel.setsToday.collect() }
        val date = "2024-03-09"
        viewModel.setDate(date)
        
        val set = ExerciseSet(id = 1L, date = date, sessionId = "s1", exerciseName = "benchpress", reps = 10, weight = 60.0, timestamp = 0L, timeStr = "12:00")
        repository.addExerciseSet(set)
        advanceUntilIdle()
        
        viewModel.deleteSet(1L, date)
        advanceUntilIdle()
        
        // Active sets should be empty
        assertEquals(0, viewModel.setsToday.value.size)
        
        // But repository should still have it as deleted
        val allSets = repository.getAllSets().first()
        assertTrue(allSets.any { it.id == 1L && it.isDeleted })
    }

    @Test
    fun testUpdateNonExistentSet() = runTest {
        backgroundScope.launch { viewModel.setsToday.collect() }
        val date = "2024-03-09"
        viewModel.setDate(date)
        
        // Try to update a set that doesn't exist
        val nonExistentSet = ExerciseSet(id = 999L, date = date, sessionId = "s1", exerciseName = "benchpress", reps = 10, weight = 60.0, timestamp = 0L, timeStr = "12:00")
        viewModel.updateSet(nonExistentSet)
        advanceUntilIdle()
        
        // Should not crash, and list should remain empty
        assertTrue(viewModel.setsToday.value.isEmpty())
    }

    @Test
    fun testRapidSetUpdates() = runTest {
        backgroundScope.launch { viewModel.setsToday.collect() }
        val date = "2024-03-09"
        viewModel.setDate(date)
        
        val set = ExerciseSet(id = 1L, date = date, sessionId = "s1", exerciseName = "benchpress", reps = 10, weight = 60.0, timestamp = 0L, timeStr = "12:00")
        repository.addExerciseSet(set)
        advanceUntilIdle()
        
        // Simulate rapid updates
        viewModel.updateSet(set.copy(reps = 11))
        viewModel.updateSet(set.copy(reps = 12))
        viewModel.updateSet(set.copy(reps = 13))
        
        advanceUntilIdle()
        assertEquals(13, viewModel.setsToday.value[0].reps)
    }
}
