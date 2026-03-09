package com.fitness.ui.profile

import com.fitness.data.FakeWorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var viewModel: SettingsViewModel
    private lateinit var repository: FakeWorkoutRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWorkoutRepository()
        viewModel = SettingsViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialValues() = runTest {
        backgroundScope.launch { viewModel.themeMode.collect() }
        backgroundScope.launch { viewModel.language.collect() }
        backgroundScope.launch { viewModel.userQuote.collect() }
        
        advanceUntilIdle()
        
        assertEquals("system", viewModel.themeMode.value)
        assertEquals("en", viewModel.language.value)
        assertEquals("Stay fit with FitBot", viewModel.userQuote.value)
    }

    @Test
    fun testSetThemeMode() = runTest {
        backgroundScope.launch { viewModel.themeMode.collect() }
        
        viewModel.setThemeMode("dark")
        advanceUntilIdle()
        
        assertEquals("dark", viewModel.themeMode.value)
    }

    @Test
    fun testSetLanguage() = runTest {
        backgroundScope.launch { viewModel.language.collect() }
        
        viewModel.setLanguage("zh")
        advanceUntilIdle()
        
        assertEquals("zh", viewModel.language.value)
    }

    @Test
    fun testSetUserQuote() = runTest {
        backgroundScope.launch { viewModel.userQuote.collect() }
        
        val quote = "No pain, no gain"
        viewModel.setUserQuote(quote)
        advanceUntilIdle()
        
        assertEquals(quote, viewModel.userQuote.value)
    }
}
