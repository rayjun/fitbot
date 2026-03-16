package com.fitness.ui.plans

import com.fitness.data.FakeWorkoutRepository
import com.fitness.model.PlannedExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {
    private lateinit var viewModel: PlanViewModel
    private lateinit var repository: FakeWorkoutRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWorkoutRepository()
        viewModel = PlanViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdatePlanDay() = runTest {
        backgroundScope.launch { viewModel.currentRoutine.collect() }

        val exercises = listOf(PlannedExercise("benchpress", 3))
        viewModel.updatePlanDay(1, false, exercises)
        
        advanceUntilIdle()
        
        val routine = viewModel.currentRoutine.value
        assertEquals(1, routine.size)
        assertEquals(1, routine[0].dayOfWeek)
        assertFalse(routine[0].isRest)
        assertEquals(exercises, routine[0].exercises)
    }

    @Test
    fun testInitialRoutineIsEmpty() = runTest {
        val routine = viewModel.currentRoutine.value
        assertTrue(routine.isEmpty())
    }

    @Test
    fun testAllSetsByDate() = runTest {
        backgroundScope.launch { viewModel.allSetsByDate.collect() }

        // Initial should be empty
        assertTrue(viewModel.allSetsByDate.value.isEmpty())

        // Add some sets across different dates
        repository.addExerciseSet(com.fitness.model.ExerciseSet(1, "2024-03-01", "s1", "squat", 10, 50.0, null, null, 0, "10:00"))
        repository.addExerciseSet(com.fitness.model.ExerciseSet(2, "2024-03-01", "s1", "squat", 10, 50.0, null, null, 0, "10:05"))
        repository.addExerciseSet(com.fitness.model.ExerciseSet(3, "2024-03-02", "s2", "benchpress", 8, 60.0, null, null, 0, "11:00"))
        
        advanceUntilIdle()

        val groupedSets = viewModel.allSetsByDate.value
        assertEquals(2, groupedSets.size)
        assertEquals(2, groupedSets["2024-03-01"]?.size)
        assertEquals(1, groupedSets["2024-03-02"]?.size)
        assertEquals("benchpress", groupedSets["2024-03-02"]?.get(0)?.exerciseName)
    }
}
