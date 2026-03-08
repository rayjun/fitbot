package com.fitness

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
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
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koin.androidx.compose.koinViewModel
import com.fitness.util.getString
import com.fitness.util.LocalAppLanguage

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val language by settingsViewModel.language.collectAsState()

            // 核心修复：监听语言变化并实时应用到 Activity 句柄
            LaunchedEffect(language) {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }

            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FitnessTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalAppLanguage provides language) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val workManager = remember { WorkManager.getInstance(applicationContext) }
                val syncWorkInfos by workManager.getWorkInfosForUniqueWorkLiveData("FullSync").observeAsState()
                val isSyncing = syncWorkInfos?.any { it.state == WorkInfo.State.RUNNING } == true
                
                val items = listOf(Screen.Library, Screen.Plans, Screen.Profile)

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = items.any { it.route == currentDestination?.route }

                // 核心修复：同步系统导航栏颜色，并消除空隙
                val systemNavBarColor = if (showBottomBar) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (context as android.app.Activity).window
                        window.navigationBarColor = systemNavBarColor.toArgb()
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets(0, 0, 0, 0)
                            ) {
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon!!, contentDescription = null) },
                                        label = { Text(getString(screen.labelKey ?: screen.route)) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
            } // end CompositionLocalProvider
            } // end FitnessTheme
        }
    }
}
