package com.fitness

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.plans.DayDetailsScreen
import com.fitness.ui.theme.FitnessTheme
import com.fitness.ui.navigation.Screen
import com.fitness.util.getString

fun MainViewController() = ComposeUIViewController {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
    
    FitnessTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    val items = listOf(Screen.Library, Screen.Plans, Screen.Profile)
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = null) },
                            label = { Text(getString(screen.route)) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentScreen) {
                    Screen.Library -> {
                        ExerciseLibraryScreen(onExerciseClick = { /* TODO: Detail */ })
                    }
                    Screen.Plans -> {
                        // Placeholder for Plans
                        Text("Plans Screen Placeholder", modifier = Modifier.padding(16.dp))
                    }
                    Screen.Profile -> {
                        // Placeholder for Profile
                        Text("Profile Screen Placeholder", modifier = Modifier.padding(16.dp))
                    }
                    else -> {}
                }
            }
        }
    }
}
