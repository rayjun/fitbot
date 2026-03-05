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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

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
    val driveScope = Scope(DriveScopes.DRIVE_FILE)

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = authManager.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            lastAccount = task.result
            val hasPerm = GoogleSignIn.hasPermissions(lastAccount, driveScope)
            if (hasPerm) {
                Toast.makeText(context, context.getString(R.string.cloud_success), Toast.LENGTH_SHORT).show()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
            } else {
                Toast.makeText(context, "请在授权时勾选云端硬盘权限以开启同步", Toast.LENGTH_LONG).show()
            }
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
            val hasDrivePermission = lastAccount?.let { GoogleSignIn.hasPermissions(it, driveScope) } ?: false
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

        composable(Screen.Profile.route) {
            val hasDrivePermission = lastAccount?.let { GoogleSignIn.hasPermissions(it, driveScope) } ?: false
            ProfileScreen(
                viewModel = hiltViewModel(),
                settingsViewModel = hiltViewModel(),
                account = if (hasDrivePermission) lastAccount else null, // 没权限视为未连接，强制重新授权
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
            val hasDrivePermission = lastAccount?.let { GoogleSignIn.hasPermissions(it, driveScope) } ?: false
            SettingsScreen(
                settingsViewModel = hiltViewModel(),
                isCloudConnected = lastAccount != null && hasDrivePermission,
                isSyncing = isSyncing,
                onSyncClick = {
                    if (lastAccount != null && hasDrivePermission) {
                        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                        workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
                        Toast.makeText(context, "Sync Started", Toast.LENGTH_SHORT).show()
                    } else {
                        googleSignInLauncher.launch(authManager.getSignInIntent())
                    }
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
