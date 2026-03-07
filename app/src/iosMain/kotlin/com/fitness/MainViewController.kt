package com.fitness

import androidx.compose.ui.window.ComposeUIViewController
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.theme.FitnessTheme

fun MainViewController() = ComposeUIViewController {
    FitnessTheme {
        ExerciseLibraryScreen(
            onExerciseClick = { /* Handle in iOS shell later */ }
        )
    }
}
