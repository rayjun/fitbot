package com.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitness.model.Exercise
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.workout.WorkoutRecordingScreen
import com.fitness.ui.workout.WorkoutViewModel

sealed class Screen {
    object Library : Screen()
    data class Workout(val exercise: Exercise) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
            
            val workoutViewModel: WorkoutViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return WorkoutViewModel(applicationContext) as T
                    }
                }
            )

            when (val screen = currentScreen) {
                is Screen.Library -> {
                    ExerciseLibraryScreen(
                        onExerciseClick = { exercise ->
                            currentScreen = Screen.Workout(exercise)
                        }
                    )
                }
                is Screen.Workout -> {
                    WorkoutRecordingScreen(
                        exerciseName = screen.exercise.name,
                        viewModel = workoutViewModel,
                        onFinished = {
                            currentScreen = Screen.Library
                        }
                    )
                }
            }
        }
    }
}
