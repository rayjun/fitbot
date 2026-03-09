package com.fitness.ui.profile

import com.fitness.data.FakeWorkoutRepository
import com.fitness.model.ExerciseSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private lateinit var viewModel: ProfileViewModel
    private lateinit var repository: FakeWorkoutRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWorkoutRepository()
        viewModel = ProfileViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testHeatmapData() = runTest {
        backgroundScope.launch { viewModel.heatmapData.collect() }
        
        // Add sets for two different dates
        repository.addExerciseSet(ExerciseSet(id = 1, date = "2024-03-08", sessionId = "s1", exerciseName = "bench", reps = 10, weight = 60.0, timestamp = 0, timeStr = "12:00"))
        repository.addExerciseSet(ExerciseSet(id = 2, date = "2024-03-08", sessionId = "s1", exerciseName = "bench", reps = 10, weight = 60.0, timestamp = 0, timeStr = "12:05"))
        repository.addExerciseSet(ExerciseSet(id = 3, date = "2024-03-09", sessionId = "s2", exerciseName = "squat", reps = 10, weight = 80.0, timestamp = 0, timeStr = "13:00"))
        
        advanceUntilIdle()
        
        val heatmap = viewModel.heatmapData.value
        assertEquals(2, heatmap.size)
        assertEquals(2, heatmap["2024-03-08"])
        assertEquals(1, heatmap["2024-03-09"])
    }
}
