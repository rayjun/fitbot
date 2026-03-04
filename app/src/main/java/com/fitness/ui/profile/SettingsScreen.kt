package com.fitness.ui.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    isCloudConnected: Boolean,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onBack: () -> Unit
) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val language by settingsViewModel.language.collectAsStateWithLifecycle()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_general), fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 云同步板块
            Text("云同步 (Cloud Sync)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isCloudConnected) Color(0xFF4CAF50) else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isCloudConnected) "已连接 Google Drive" else "未连接云端",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    if (isCloudConnected) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSyncClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                Icon(Icons.Default.Sync, null, modifier = Modifier.rotate(rotation))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("同步中...")
                            } else {
                                Icon(Icons.Default.Sync, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("立即手动同步")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.settings_general), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Column {
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
    }

    // Dialogs ... (Same as before)
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
