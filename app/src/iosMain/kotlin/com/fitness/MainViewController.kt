package com.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.library.ExerciseDetailScreen
import com.fitness.ui.plans.PlansScreen
import com.fitness.ui.profile.ProfileScreen
import com.fitness.ui.profile.SettingsScreen
import com.fitness.ui.theme.FitnessTheme
import com.fitness.ui.navigation.Screen
import com.fitness.model.Exercise
import com.fitness.data.WorkoutRepository
import com.fitness.auth.AuthManager
import com.fitness.util.LocalAppLanguage
import com.fitness.util.getString
import com.fitness.ui.plans.PlanViewModel
import com.fitness.ui.workout.WorkoutViewModel
import com.fitness.ui.profile.SettingsViewModel
import com.fitness.ui.profile.ProfileViewModel
import com.fitness.di.initKoin
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.mp.KoinPlatform

// Call setupKoin() from Swift App.init() to ensure Koin is initialized once
// before any Compose UI is created.
fun setupKoin() = initKoin()

// Returns the Koin-managed AuthManager singleton so Swift can register its
// GoogleSignInBridge launcher before the UI starts.
fun getAuthManager(): AuthManager = KoinPlatform.getKoin().get()

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
fun MainViewController() = ComposeUIViewController {
    KoinContext {
        val scope = rememberCoroutineScope()
        val repository: WorkoutRepository = koinInject()
        val authManager: AuthManager = koinInject()

        // --- ViewModels ---
        val planViewModel: PlanViewModel = koinViewModel()
        val settingsViewModel: SettingsViewModel = koinViewModel()
        val workoutViewModel: WorkoutViewModel = koinViewModel()
        val profileViewModel: ProfileViewModel = koinViewModel()

        LaunchedEffect(Unit) {
            authManager.restoreSignIn()
        }

        // --- UI State ---
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
        var previousScreen by remember { mutableStateOf<Screen?>(null) }
        var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
        var workoutExerciseId by remember { mutableStateOf<String?>(null) }
        var workoutDate by remember { mutableStateOf<String?>(null) }
        var dayDetailsDate by remember { mutableStateOf<String?>(null) }

        // Observable Logic Data
        val routine by planViewModel.currentRoutine.collectAsState()
        val allSetsByDate by planViewModel.allSetsByDate.collectAsState()
        val heatmapData by profileViewModel.heatmapData.collectAsState()
        val userProfile by authManager.currentUser.collectAsState()
        val isSyncing by authManager.isSyncing.collectAsState()

        // Settings State
        val themeMode by settingsViewModel.themeMode.collectAsState()
        val language by settingsViewModel.language.collectAsState()
        val userQuote by settingsViewModel.userQuote.collectAsState()

        val isDark = when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }

        // Provide the app-level language setting to the entire Compose tree.
        // getString() reads LocalAppLanguage to resolve strings from the correct
        // language dictionary, bypassing NSLocale (which Compose Resources uses by default).
        CompositionLocalProvider(LocalAppLanguage provides language) {
            FitnessTheme(darkTheme = isDark) {
                Scaffold(
                    bottomBar = {
                        val items = listOf(Screen.Library, Screen.Plans, Screen.Profile)
                        if (items.any { it.route == currentScreen.route } && selectedExercise == null && workoutExerciseId == null && dayDetailsDate == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .height(48.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items.forEach { screen ->
                                    val selected = currentScreen.route == screen.route
                                    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { currentScreen = screen }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(screen.icon!!, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(getString(screen.labelKey ?: screen.route), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (workoutExerciseId != null && workoutDate != null) {
                            com.fitness.ui.workout.WorkoutRecordingScreen(
                                exerciseId = workoutExerciseId!!,
                                date = workoutDate!!,
                                repository = repository,
                                onBack = {
                                    workoutExerciseId = null
                                    workoutDate = null
                                }
                            )
                        } else if (dayDetailsDate != null) {
                            val setsByDate by repository.getSetsByDate(dayDetailsDate!!).collectAsState(initial = emptyList())
                            com.fitness.ui.plans.DayDetailsScreen(
                                date = dayDetailsDate!!,
                                sets = setsByDate,
                                onBack = { dayDetailsDate = null }
                            )
                        } else if (selectedExercise != null) {
                            ExerciseDetailScreen(
                                exerciseId = selectedExercise!!.id,
                                onBack = { selectedExercise = null }
                            )
                        } else {
                            when (currentScreen) {
                                Screen.Library -> {
                                    ExerciseLibraryScreen(onExerciseClick = { selectedExercise = it })
                                }
                                Screen.Plans -> {
                                    PlansScreen(
                                        currentRoutine = routine,
                                        setsByDate = allSetsByDate,
                                        onStartExercise = { exId, date ->
                                            workoutExerciseId = exId
                                            workoutDate = date
                                        },
                                        onDayClick = { date ->
                                            dayDetailsDate = date
                                        },
                                        onUpdatePlanDay = { day, isRest, exList ->
                                            planViewModel.updatePlanDay(day, isRest, exList)
                                        }
                                    )
                                }
                                Screen.Profile -> {
                                    ProfileScreen(
                                        userQuote = userQuote,
                                        heatmapData = heatmapData,
                                        accountName = userProfile?.name,
                                        accountPhotoUrl = userProfile?.photoUrl,
                                        onLoginClick = {
                                            scope.launch { authManager.signIn() }
                                        },
                                        onLogout = {
                                            scope.launch { authManager.signOut() }
                                        },
                                        onSettingsClick = {
                                            previousScreen = Screen.Profile
                                            currentScreen = Screen.Settings
                                        },
                                        onUpdateQuote = { settingsViewModel.setUserQuote(it) }
                                    )
                                }
                                Screen.Settings -> {
                                    SettingsScreen(
                                        themeMode = themeMode,
                                        language = language,
                                        isCloudConnected = userProfile != null,
                                        isSyncing = isSyncing,
                                        onSyncClick = { scope.launch { authManager.sync() } },
                                        onBack = {
                                            currentScreen = previousScreen ?: Screen.Profile
                                        },
                                        onThemeChange = { settingsViewModel.setThemeMode(it) },
                                        onLanguageChange = { settingsViewModel.setLanguage(it) }
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
