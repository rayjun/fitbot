package com.fitness.ui.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fitness.R
import com.fitness.sync.AuthManager
import com.fitness.sync.SyncWorker
import com.fitness.ui.library.ExerciseDetailScreen
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.plans.DayDetailsScreen
import com.fitness.ui.plans.PlanSessionScreen
import com.fitness.ui.plans.PlansScreen
import com.fitness.ui.profile.ProfileScreen
import com.fitness.ui.profile.SettingsScreen
import com.fitness.ui.workout.WorkoutRecordingScreen
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

@Composable
fun FitBotNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    authManager: AuthManager,
    isSyncing: Boolean,
    workManager: WorkManager
) {
    val context = LocalContext.current
    var lastAccount by remember { mutableStateOf(authManager.getSignedInAccount()) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = authManager.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            lastAccount = task.result
            Toast.makeText(context, context.getString(R.string.cloud_success), Toast.LENGTH_SHORT).show()
            // 登录成功后立即触发一次全量同步，确保文件夹被创建且数据被对齐
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
            workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
        } else {
            Toast.makeText(context, context.getString(R.string.cloud_failed, task.exception?.message), Toast.LENGTH_LONG).show()
        }
    }

    NavHost(
        navController = navController, 
        startDestination = Screen.Library.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Library.route) {
            ExerciseLibraryScreen(
                onExerciseClick = { exercise ->
                    navController.navigate(Screen.ExerciseDetail.createRoute(exercise.id))
                }
            )
        }
        
        composable(
            route = Screen.ExerciseDetail.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Plans.route) {
            PlansScreen(
                viewModel = hiltViewModel(),
                workoutViewModel = hiltViewModel(),
                onStartExercise = { exerciseId, date ->
                    navController.navigate(Screen.Workout.createRoute(exerciseId, date))
                },
                onDayClick = { date ->
                    navController.navigate(Screen.DayDetails.createRoute(date))
                }
            )
        }

        composable(
            route = Screen.DayDetails.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            DayDetailsScreen(
                date = date,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlanSession.route,
            arguments = listOf(navArgument("dayOfWeek") { type = NavType.IntType })
        ) { backStackEntry ->
            val dayOfWeek = backStackEntry.arguments?.getInt("dayOfWeek") ?: 0
            PlanSessionScreen(
                dayOfWeek = dayOfWeek,
                planViewModel = hiltViewModel(),
                workoutViewModel = hiltViewModel(),
                onExerciseClick = { exercise ->
                    // Fallback for old navigation if any, though Plans tab is updated
                    val today = java.time.LocalDate.now().toString()
                    navController.navigate(Screen.Workout.createRoute(exercise.id, today))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = hiltViewModel(),
                settingsViewModel = hiltViewModel(),
                account = lastAccount,
                onLoginClick = {
                    googleSignInLauncher.launch(authManager.getSignInIntent())
                },
                onLogout = {
                    authManager.signOut {
                        lastAccount = null
                        Toast.makeText(context, context.getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsViewModel = hiltViewModel(),
                isCloudConnected = lastAccount != null,
                isSyncing = isSyncing,
                onSyncClick = {
                    val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                    workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
                    Toast.makeText(context, "Sync Started", Toast.LENGTH_SHORT).show()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Workout.route,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            WorkoutRecordingScreen(
                exerciseId = exerciseId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() }
            )
        }
    }
}
