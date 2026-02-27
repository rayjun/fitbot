package com.fitness

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.navigation.Screen
import com.fitness.ui.plans.PlanViewModel
import com.fitness.ui.plans.PlansScreen
import com.fitness.ui.profile.ProfileScreen
import com.fitness.ui.workout.WorkoutRecordingScreen
import com.fitness.ui.workout.WorkoutViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            
            // Google Login State
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

            val planViewModel: PlanViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PlanViewModel(applicationContext) as T
                    }
                }
            )

            val items = listOf(Screen.Library, Screen.Plans, Screen.Profile)

            Scaffold(
                bottomBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    // 只有在主 Tab 页面显示底部导航栏
                    val showBottomBar = items.any { it.route == currentDestination?.route }
                    
                    if (showBottomBar) {
                        NavigationBar {
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon!!, contentDescription = null) },
                                    label = { Text(screen.title!!) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController, 
                    startDestination = Screen.Library.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Library.route) {
                        ExerciseLibraryScreen(
                            isCloudConnected = lastAccount != null,
                            onConnectCloud = {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                                    .build()
                                val client = GoogleSignIn.getClient(this@MainActivity, gso)
                                googleSignInLauncher.launch(client.signInIntent)
                            },
                            onExerciseClick = { exercise ->
                                navController.navigate(Screen.Workout.createRoute(exercise.name))
                            }
                        )
                    }
                    composable(Screen.Plans.route) {
                        PlansScreen(planViewModel)
                    }
                    composable(Screen.Profile.route) {
                        ProfileScreen()
                    }
                    composable(
                        route = Screen.Workout.route,
                        arguments = listOf(navArgument("exerciseName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val exerciseName = backStackEntry.arguments?.getString("exerciseName") ?: ""
                        WorkoutRecordingScreen(
                            exerciseName = exerciseName,
                            viewModel = workoutViewModel,
                            onBack = { navController.popBackStack() },
                            onFinished = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
