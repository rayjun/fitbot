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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.fitness.util.toModel
import com.fitness.ui.library.ExerciseDetailScreen
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.plans.DayDetailsScreen
import com.fitness.ui.plans.PlansScreen
import com.fitness.ui.profile.ProfileScreen
import com.fitness.ui.profile.SettingsScreen
import com.fitness.ui.workout.WorkoutRecordingScreen
import com.fitness.ui.workout.WorkoutViewModel
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
            val planViewModel: com.fitness.ui.plans.PlanViewModel = hiltViewModel()
            val workoutViewModel: com.fitness.ui.workout.WorkoutViewModel = hiltViewModel()
            val routine by planViewModel.currentRoutine.collectAsStateWithLifecycle()
            val setsToday by workoutViewModel.setsToday.collectAsStateWithLifecycle()
            
            val todayStr = java.time.LocalDate.now().toString()

            PlansScreen(
                currentRoutine = routine,
                setsByDate = mapOf(todayStr to setsToday.map { (it as com.fitness.data.local.SetEntity).toModel() }),
                onStartExercise = { exerciseId, date ->
                    navController.navigate(Screen.Workout.createRoute(exerciseId, date))
                },
                onDayClick = { date ->
                    navController.navigate(Screen.DayDetails.createRoute(date))
                },
                onUpdatePlanDay = { day, isRest, ex ->
                    planViewModel.updatePlanDay(day, isRest, ex)
                }
            )
        }

        composable(
            route = Screen.DayDetails.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val workoutViewModel: WorkoutViewModel = hiltViewModel()
            val sets by produceState(initialValue = emptyList<com.fitness.model.ExerciseSet>(), date) {
                workoutViewModel.getSetsByDateFlow(date).collect { list ->
                    value = list.map { it.toModel() }
                }
            }
            DayDetailsScreen(
                date = date,
                sets = sets,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel: com.fitness.ui.profile.ProfileViewModel = hiltViewModel()
            val settingsViewModel: com.fitness.ui.profile.SettingsViewModel = hiltViewModel()
            val heatmapData by profileViewModel.heatmapData.collectAsStateWithLifecycle()
            val userQuote by settingsViewModel.userQuote.collectAsStateWithLifecycle()
            val hasDrivePermission = lastAccount?.let { GoogleSignIn.hasPermissions(it, driveScope) } ?: false
            
            ProfileScreen(
                userQuote = userQuote,
                heatmapData = heatmapData,
                accountName = lastAccount?.displayName,
                accountPhotoUrl = lastAccount?.photoUrl?.toString(),
                onLoginClick = { triggerAuthFlow() },
                onLogout = {
                    authManager.signOut {
                        lastAccount = null
                        Toast.makeText(context, context.getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onUpdateQuote = { settingsViewModel.setUserQuote(it) }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: com.fitness.ui.profile.SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val language by settingsViewModel.language.collectAsStateWithLifecycle()
            val hasDrivePermission = lastAccount?.let { GoogleSignIn.hasPermissions(it, driveScope) } ?: false
            
            SettingsScreen(
                themeMode = themeMode,
                language = language,
                isCloudConnected = lastAccount != null && hasDrivePermission,
                isSyncing = isSyncing,
                onSyncClick = {
                    if (lastAccount != null && hasDrivePermission) {
                        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                        workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
                        Toast.makeText(context, "Sync Started", Toast.LENGTH_SHORT).show()
                    } else {
                        triggerAuthFlow()
                    }
                },
                onBack = { navController.popBackStack() },
                onThemeChange = { settingsViewModel.setThemeMode(it) },
                onLanguageChange = { settingsViewModel.setLanguage(it) }
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
