package com.fitness

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.core.okio.OkioSerializer
import com.fitness.ui.library.ExerciseLibraryScreen
import com.fitness.ui.library.ExerciseDetailScreen
import com.fitness.ui.plans.PlansScreen
import com.fitness.ui.profile.ProfileScreen
import com.fitness.ui.profile.SettingsScreen
import com.fitness.ui.theme.FitnessTheme
import com.fitness.ui.navigation.Screen
import com.fitness.model.Exercise
import com.fitness.data.DataStoreRepository
import com.fitness.auth.AuthManager
import com.fitness.util.getString
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import okio.Path.Companion.toPath
import okio.FileSystem
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    val scope = rememberCoroutineScope()
    
    // --- Data Layer Initialization (iOS) ---
    val repository = remember {
        val fileManager = NSFileManager.defaultManager
        val documentDirectory: NSURL? = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        val path = (documentDirectory?.path ?: "") + "/fitness_settings.preferences_pb"
        
        val dataStore = PreferenceDataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = androidx.datastore.preferences.core.PreferencesSerializer,
                producePath = { path.toPath() }
            )
        )
        DataStoreRepository(dataStore)
    }
    
    val authManager = remember { AuthManager() }
    
    // --- UI State ---
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var workoutExerciseId by remember { mutableStateOf<String?>(null) }
    var workoutDate by remember { mutableStateOf<String?>(null) }
    var dayDetailsDate by remember { mutableStateOf<String?>(null) }
    
    // Observable Logic Data
    val routine by repository.getCurrentRoutine().collectAsState(initial = emptyList())
    val userProfile by authManager.currentUser.collectAsState()
    val isSyncing by authManager.isSyncing.collectAsState()
    
    // Settings State
    var themeMode by remember { mutableStateOf("system") }
    var language by remember { mutableStateOf("en") }
    var userQuote by remember { mutableStateOf("Stay fit with FitBot") }

    val todayStr = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
    val setsToday by repository.getSetsByDate(todayStr).collectAsState(initial = emptyList())

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    FitnessTheme(darkTheme = isDark) {
        Scaffold(
            bottomBar = {
                val items = listOf(Screen.Library, Screen.Plans, Screen.Profile)
                if (items.any { it.route == currentScreen.route } && selectedExercise == null && workoutExerciseId == null && dayDetailsDate == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon!!, contentDescription = null) },
                                label = { Text(getString(screen.route)) },
                                selected = currentScreen.route == screen.route,
                                onClick = { currentScreen = screen }
                            )
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
                                setsByDate = mapOf(todayStr to setsToday),
                                onStartExercise = { exId, date ->
                                    workoutExerciseId = exId
                                    workoutDate = date
                                },
                                onDayClick = { date -> 
                                    dayDetailsDate = date 
                                },
                                onUpdatePlanDay = { day, isRest, exList ->
                                    scope.launch {
                                        repository.updateRoutineDay(day, isRest, exList)
                                    }
                                }
                            )
                        }
                        Screen.Profile -> {
                            ProfileScreen(
                                userQuote = userQuote,
                                heatmapData = emptyMap(),
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
                                onUpdateQuote = { userQuote = it }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                themeMode = themeMode,
                                language = language,
                                isCloudConnected = userProfile != null,
                                isSyncing = isSyncing,
                                onSyncClick = { },
                                onBack = { 
                                    currentScreen = previousScreen ?: Screen.Profile 
                                },
                                onThemeChange = { themeMode = it },
                                onLanguageChange = { language = it }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
