package com.fitness.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fitness.model.PlannedExercise
import com.fitness.sync.AuthManager
import com.fitness.sync.SyncWorker
import com.fitness.ui.library.ExerciseDetailScreen
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.plans.DayDetailsScreen
import com.fitness.ui.plans.PlansScreen
import com.fitness.ui.profile.ProfileScreen
import com.fitness.ui.profile.SettingsScreen
import com.fitness.ui.plans.PlanViewModel
import com.fitness.ui.workout.WorkoutViewModel
import com.fitness.ui.profile.SettingsViewModel
import com.fitness.ui.profile.ProfileViewModel
import com.fitness.util.getString
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import org.koin.androidx.compose.koinViewModel
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.fitness.R
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
            val account = task.result
            lastAccount = account
            val hasPerm = GoogleSignIn.hasPermissions(account, driveScope)
            if (hasPerm) {
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
            }
        }
    }

    // Silent sign in on launch
    LaunchedEffect(Unit) {
        val account = authManager.getSignedInAccount()
        if (account != null) {
            lastAccount = account
            if (GoogleSignIn.hasPermissions(account, driveScope)) {
                Log.d("FitBotSync", "Auto-sync on startup for ${account.email}")
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                workManager.enqueueUniqueWork("FullSync", ExistingWorkPolicy.REPLACE, syncRequest)
            }
        }
    }

    val triggerAuthFlow = {
        googleSignInLauncher.launch(authManager.getSignInIntent())
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
            val planViewModel: PlanViewModel = koinViewModel()
            val routine by planViewModel.currentRoutine.collectAsState()
            val allSetsByDate by planViewModel.allSetsByDate.collectAsState()

            PlansScreen(
                currentRoutine = routine,
                setsByDate = allSetsByDate,
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
            val workoutViewModel: WorkoutViewModel = koinViewModel()
            LaunchedEffect(date) {
                workoutViewModel.setDate(date)
            }
            val sets by workoutViewModel.setsToday.collectAsState()
            DayDetailsScreen(
                date = date,
                sets = sets,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val profileViewModel: ProfileViewModel = koinViewModel()
            val userQuote by settingsViewModel.userQuote.collectAsState()
            val heatmapData by profileViewModel.heatmapData.collectAsState()
            
            ProfileScreen(
                userQuote = userQuote,
                heatmapData = heatmapData,
                accountName = lastAccount?.displayName,
                accountPhotoUrl = lastAccount?.photoUrl?.toString(),
                onLoginClick = { triggerAuthFlow() },
                onLogout = {
                    authManager.signOut {
                        lastAccount = null
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAnalyticsClick = {
                    navController.navigate(Screen.Analytics.route)
                },
                onUpdateQuote = { settingsViewModel.setUserQuote(it) }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val language by settingsViewModel.language.collectAsState()
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
                    } else {
                        triggerAuthFlow()
                    }
                },
                onBack = { navController.popBackStack() },
                onThemeChange = { settingsViewModel.setThemeMode(it) },
                onLanguageChange = { settingsViewModel.setLanguage(it) }
            )
        }

        composable(Screen.Analytics.route) {
            val profileViewModel: ProfileViewModel = koinViewModel()
            val muscleVolumeData by profileViewModel.muscleVolumeData.collectAsState(initial = emptyMap())
            
            com.fitness.ui.profile.AnalyticsScreen(
                muscleVolumeData = muscleVolumeData,
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
            val date = backStackEntry.arguments?.getString("date") ?: ""
            
            com.fitness.ui.workout.WorkoutRecordingScreen(
                exerciseId = exerciseId,
                date = date,
                repository = org.koin.compose.getKoin().get(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
