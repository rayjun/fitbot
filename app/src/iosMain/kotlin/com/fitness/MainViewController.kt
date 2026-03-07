package com.fitness

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.library.ExerciseDetailScreen
import com.fitness.ui.plans.PlansScreen
import com.fitness.ui.profile.ProfileScreen
import com.fitness.ui.profile.SettingsScreen
import com.fitness.ui.theme.FitnessTheme
import com.fitness.ui.navigation.Screen
import com.fitness.model.Exercise
import com.fitness.util.getString

fun MainViewController() = ComposeUIViewController {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    
    // Simple state for demo
    var themeMode by remember { mutableStateOf("system") }
    var language by remember { mutableStateOf("en") }
    var userQuote by remember { mutableStateOf("Stay fit with FitBot") }

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    FitnessTheme(darkTheme = isDark) {
        Scaffold(
            bottomBar = {
                val items = listOf(Screen.Library, Screen.Plans, Screen.Profile)
                if (items.any { it.route == currentScreen.route } && selectedExercise == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon!!, contentDescription = null) },
                                label = { Text(getString(screen.route)) },
                                selected = currentScreen.route == screen.route,
                                onClick = { currentScreen = screen }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                if (selectedExercise != null) {
                    ExerciseDetailScreen(
                        exerciseId = selectedExercise!!.id,
                        onBack = { selectedExercise = null }
                    )
                } else {
                    when (currentScreen) {
                        Screen.Library -> {
                            ExerciseLibraryScreen(onExerciseClick = { selectedExercise = it })
                        }
                        Screen.Plans -> {
                            PlansScreen(
                                currentRoutine = emptyList(),
                                setsByDate = emptyMap(),
                                onStartExercise = { _, _ -> },
                                onDayClick = { _ -> },
                                onUpdatePlanDay = { _, _, _ -> }
                            )
                        }
                        Screen.Profile -> {
                            ProfileScreen(
                                userQuote = userQuote,
                                heatmapData = emptyMap(),
                                accountName = "iOS User",
                                accountPhotoUrl = null,
                                onLoginClick = { },
                                onLogout = { },
                                onSettingsClick = { 
                                    previousScreen = Screen.Profile
                                    currentScreen = Screen.Settings 
                                },
                                onUpdateQuote = { userQuote = it }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                themeMode = themeMode,
                                language = language,
                                isCloudConnected = false,
                                isSyncing = false,
                                onSyncClick = { },
                                onBack = { 
                                    currentScreen = previousScreen ?: Screen.Profile 
                                },
                                onThemeChange = { themeMode = it },
                                onLanguageChange = { language = it }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
