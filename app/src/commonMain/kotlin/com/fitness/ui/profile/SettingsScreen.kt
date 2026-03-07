package com.fitness.ui.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.util.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    language: String,
    isCloudConnected: Boolean,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onBack: () -> Unit,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit
) {
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
                title = { Text(getString("settings_title"), fontWeight = FontWeight.Bold) },
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
            Text(getString("cloud_sync_title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            text = if (isCloudConnected) getString("cloud_connected") else getString("cloud_disconnected"),
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
                                Text(getString("syncing"))
                            } else {
                                Icon(Icons.Default.Sync, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(getString("sync_now"))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(getString("settings_general"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Column {
                SettingsItem(
                    icon = Icons.Default.Settings, 
                    title = getString("settings_theme"), 
                    supportingText = when (themeMode) {
                        "dark" -> getString("theme_dark")
                        "light" -> getString("theme_light")
                        else -> getString("theme_system")
                    }
                ) {
                    showThemeDialog = true
                }

                SettingsItem(
                    icon = Icons.Default.Language, 
                    title = getString("settings_language"), 
                    supportingText = if (language == "zh") getString("lang_zh") else getString("lang_en")
                ) {
                    showLanguageDialog = true
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(getString("settings_language")) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(getString("lang_zh")) },
                        modifier = Modifier.clickable {
                            onLanguageChange("zh")
                            showLanguageDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(getString("lang_en")) },
                        modifier = Modifier.clickable {
                            onLanguageChange("en")
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(getString("dialog_cancel")) }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(getString("settings_theme")) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(getString("theme_system")) },
                        modifier = Modifier.clickable {
                            onThemeChange("system")
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(getString("theme_light")) },
                        modifier = Modifier.clickable {
                            onThemeChange("light")
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(getString("theme_dark")) },
                        modifier = Modifier.clickable {
                            onThemeChange("dark")
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(getString("dialog_cancel")) }
            }
        )
    }
}
