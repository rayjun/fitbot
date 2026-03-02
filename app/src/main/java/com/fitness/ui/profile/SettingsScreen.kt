package com.fitness.ui.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val language by settingsViewModel.language.collectAsStateWithLifecycle()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_general)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.settings_general), fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(
                icon = Icons.Default.Settings, 
                title = stringResource(R.string.settings_theme), 
                supportingText = when (themeMode) {
                    "dark" -> stringResource(R.string.theme_dark)
                    "light" -> stringResource(R.string.theme_light)
                    else -> stringResource(R.string.theme_system)
                }
            ) {
                showThemeDialog = true
            }

            SettingsItem(
                icon = Icons.Default.Language, 
                title = stringResource(R.string.settings_language), 
                supportingText = if (language == "zh") stringResource(R.string.lang_zh) else stringResource(R.string.lang_en)
            ) {
                showLanguageDialog = true
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.lang_zh)) },
                        modifier = Modifier.clickable {
                            settingsViewModel.setLanguage("zh")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh"))
                            showLanguageDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.lang_en)) },
                        modifier = Modifier.clickable {
                            settingsViewModel.setLanguage("en")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_system)) },
                        modifier = Modifier.clickable {
                            settingsViewModel.setThemeMode("system")
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_light)) },
                        modifier = Modifier.clickable {
                            settingsViewModel.setThemeMode("light")
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_dark)) },
                        modifier = Modifier.clickable {
                            settingsViewModel.setThemeMode("dark")
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
}
