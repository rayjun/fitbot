package com.fitness.ui.profile

import com.fitness.data.FakeWorkoutRepository
import com.fitness.model.ExerciseSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
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

    private fun getOffsetDate(days: Int): String {
        val now = Clock.System.now()
        return now.minus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }

    @Test
    fun testTimeRangeFiltering() = runTest {
        backgroundScope.launch { viewModel.muscleVolumeData.collect() }
        backgroundScope.launch { viewModel.selectedTimeRange.collect() }

        // 1. Add records at different times
        val today = getOffsetDate(0)
        val tenDaysAgo = getOffsetDate(10)
        val twoYearsAgo = getOffsetDate(400)

        repository.addExerciseSet(ExerciseSet(id = 1, date = today, sessionId = "s1", exerciseName = "benchpress", reps = 10, weight = 100.0, timestamp = 0, timeStr = "12:00"))
        repository.addExerciseSet(ExerciseSet(id = 2, date = tenDaysAgo, sessionId = "s2", exerciseName = "benchpress", reps = 10, weight = 100.0, timestamp = 0, timeStr = "12:00"))
        repository.addExerciseSet(ExerciseSet(id = 3, date = twoYearsAgo, sessionId = "s3", exerciseName = "benchpress", reps = 10, weight = 100.0, timestamp = 0, timeStr = "12:00"))
        
        advanceUntilIdle()

        // 2. Default is MONTH (30 days), should see Today and 10 days ago (Total 2000.0)
        assertEquals(2000.0, viewModel.muscleVolumeData.value["cat_chest"])

        // 3. Switch to WEEK (7 days), should only see Today (1000.0)
        viewModel.setSelectedTimeRange(TimeRange.WEEK)
        advanceUntilIdle()
        assertEquals(1000.0, viewModel.muscleVolumeData.value["cat_chest"])

        // 4. Switch to YEAR (365 days), should see Today and 10 days ago (2000.0)
        viewModel.setSelectedTimeRange(TimeRange.YEAR)
        advanceUntilIdle()
        assertEquals(2000.0, viewModel.muscleVolumeData.value["cat_chest"])
    }

    @Test
    fun testCompositeFiltering() = runTest {
        backgroundScope.launch { viewModel.muscleVolumeData.collect() }
        
        val today = getOffsetDate(0)
        val tenDaysAgo = getOffsetDate(10)

        // Chest today, Legs 10 days ago
        repository.addExerciseSet(ExerciseSet(id = 1, date = today, sessionId = "s1", exerciseName = "benchpress", reps = 10, weight = 100.0, timestamp = 0, timeStr = "12:00"))
        repository.addExerciseSet(ExerciseSet(id = 2, date = tenDaysAgo, sessionId = "s2", exerciseName = "squat", reps = 10, weight = 100.0, timestamp = 0, timeStr = "12:00"))
        
        advanceUntilIdle()

        // Filter: WEEK (only today) AND Category: Legs
        viewModel.setSelectedTimeRange(TimeRange.WEEK)
        viewModel.setSelectedCategory("cat_legs")
        advanceUntilIdle()
        
        // Should be empty because the only leg workout was 10 days ago
        assertTrue(viewModel.muscleVolumeData.value.isEmpty())

        // Filter: MONTH (includes 10 days ago) AND Category: Legs
        viewModel.setSelectedTimeRange(TimeRange.MONTH)
        advanceUntilIdle()
        assertTrue(viewModel.muscleVolumeData.value.containsKey("cat_legs"))
        assertEquals(1000.0, viewModel.muscleVolumeData.value["cat_legs"])
    }
}
