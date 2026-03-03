package com.fitness

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.compose.runtime.livedata.observeAsState
import com.fitness.ui.navigation.FitBotNavHost
import com.fitness.ui.navigation.Screen
import com.fitness.ui.profile.SettingsViewModel
import com.fitness.ui.theme.FitnessTheme
import com.fitness.sync.AuthManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FitnessTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val workManager = remember { WorkManager.getInstance(applicationContext) }
                val syncWorkInfos by workManager.getWorkInfosForUniqueWorkLiveData("FullSync").observeAsState()
                val isSyncing = syncWorkInfos?.any { it.state == WorkInfo.State.RUNNING } == true
                
                val items = listOf(Screen.Library, Screen.Plans, Screen.Profile)

                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        val showBottomBar = items.any { it.route == currentDestination?.route }
                        
                        if (showBottomBar) {
                            NavigationBar {
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon!!, contentDescription = null) },
                                        label = { 
                                            Text(
                                                when(screen) {
                                                    Screen.Library -> stringResource(R.string.nav_library)
                                                    Screen.Plans -> stringResource(R.string.nav_plans)
                                                    Screen.Profile -> stringResource(R.string.nav_profile)
                                                    else -> ""
                                                }
                                            ) 
                                        },
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
                    FitBotNavHost(
                        navController = navController,
                        innerPadding = innerPadding,
                        authManager = authManager,
                        isSyncing = isSyncing,
                        workManager = workManager
                    )
                }
            }
        }
    }
}
