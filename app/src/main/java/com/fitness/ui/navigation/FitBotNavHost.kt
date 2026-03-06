package com.fitness.ui.navigation

import android.util.Log
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

    // 统一的登录结果处理器
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = authManager.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            lastAccount = account
            val hasPerm = GoogleSignIn.hasPermissions(account, driveScope)
            Log.d("FitBotSync", "Login successful. Drive permission: $hasPerm")
            if (hasPerm) {
                Toast.makeText(context, context.getString(R.string.cloud_success), Toast.LENGTH_SHORT).show()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
            } else {
                Toast.makeText(context, "同步失败：您必须勾选并允许 Drive 访问权限才能使用云同步功能。", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("FitBotSync", "Login failed: ${task.exception?.message}")
            Toast.makeText(context, context.getString(R.string.cloud_failed, task.exception?.message), Toast.LENGTH_LONG).show()
        }
    }

    // 彻底强制重新授权：撤销权限并退出，确保下次登录显示完整勾选框
    val triggerAuthFlow = {
        authManager.revokeAccess {
            googleSignInLauncher.launch(authManager.getSignInIntent())
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

        composable(Screen.Profile.route) {
            val hasDrivePermission = lastAccount?.let { GoogleSignIn.hasPermissions(it, driveScope) } ?: false
            ProfileScreen(
                viewModel = hiltViewModel(),
                settingsViewModel = hiltViewModel(),
                account = if (hasDrivePermission) lastAccount else null,
                onLoginClick = { triggerAuthFlow() }, // 始终强制刷新流程
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
                        triggerAuthFlow() // 缺失权限时强制重新授权
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
