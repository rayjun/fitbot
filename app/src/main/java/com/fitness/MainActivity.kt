package com.fitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitness.model.Exercise
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.workout.WorkoutRecordingScreen
import com.fitness.ui.workout.WorkoutViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

sealed class Screen {
    object Library : Screen()
    data class Workout(val exercise: Exercise) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
            val context = LocalContext.current
            
            // 检查当前是否已登录
            var lastAccount by remember { 
                mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) 
            }
            
            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (task.isSuccessful) {
                    lastAccount = task.result
                    Toast.makeText(context, "云盘连接成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "连接失败: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }

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
                        },
                        isCloudConnected = lastAccount != null,
                        onConnectCloud = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                                .build()
                            val client = GoogleSignIn.getClient(this, gso)
                            googleSignInLauncher.launch(client.signInIntent)
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
