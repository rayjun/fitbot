package com.fitness.ui.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fitness.R
import com.fitness.data.ExerciseProvider
import com.fitness.data.local.SetEntity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel, 
    settingsViewModel: SettingsViewModel,
    account: GoogleSignInAccount?,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit
) {
    val heatmapData by viewModel.heatmapData.collectAsStateWithLifecycle()
    val allHistorySets by viewModel.allHistorySets.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val language by settingsViewModel.language.collectAsStateWithLifecycle()
    val userQuote by settingsViewModel.userQuote.collectAsStateWithLifecycle()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQuoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_profile)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // User Info Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    if (account == null) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onLoginClick) {
                                Text(stringResource(R.string.login_drive))
                            }
                        }
                    } else {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (account.photoUrl != null) {
                                AsyncImage(
                                    model = account.photoUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(64.dp).clip(CircleShape)
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(account.displayName?.firstOrNull()?.toString() ?: "U", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.displayName ?: "User", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showQuoteDialog = true }.padding(top = 4.dp)
                                ) {
                                    Text(userQuote, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_quote), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Heatmap
            item {
                Text(stringResource(R.string.heatmap_title), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    WorkoutHeatMap(heatmapData)
                }
            }

            // Settings
            item {
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

            // History Section
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            if (allHistorySets.isEmpty()) {
                item {
                    Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                val grouped = allHistorySets.groupBy { it.date }
                grouped.forEach { (date, dailySets) ->
                    item {
                        Text(date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    val exercisesInDay = dailySets.groupBy { it.exerciseName }
                    items(exercisesInDay.toList()) { (exerciseId, exerciseSets) ->
                        val exercise = ExerciseProvider.exercises.find { it.id == exerciseId }
                        val name = exercise?.let { stringResource(it.nameRes) } ?: exerciseId
                        ListItem(
                            headlineContent = { Text(name, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                Text(exerciseSets.joinToString(" | ") { "${it.weight}kg x ${it.reps}" })
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Logout
            if (account != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer, 
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.logout))
                    }
                }
            }
        }
    }

    // Dialogs ... (keep existing)
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

    if (showQuoteDialog) {
        var tempQuote by remember { mutableStateOf(userQuote) }
        AlertDialog(
            onDismissRequest = { showQuoteDialog = false },
            title = { Text(stringResource(R.string.edit_quote)) },
            text = {
                OutlinedTextField(
                    value = tempQuote,
                    onValueChange = { tempQuote = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    settingsViewModel.setUserQuote(tempQuote)
                    showQuoteDialog = false 
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showQuoteDialog = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
}

@Composable
fun WorkoutHeatMap(data: Map<String, Int>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSpacing = 4.dp
        val cellSize = 13.dp
        val columnWidth = cellSize + cellSpacing
        val columns = (maxWidth / columnWidth).toInt()
        val totalDays = columns * 7

        val days = remember(totalDays) {
            val list = mutableListOf<Date>()
            val cal = Calendar.getInstance()
            for (i in 0 until totalDays) {
                list.add(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            list.reversed()
        }
        
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val chunks = days.chunked(7)
            chunks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                    week.forEach { day ->
                        val dateStr = df.format(day)
                        val count = data[dateStr] ?: 0
                        val color = when {
                            count == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            count < 5 -> Color(0xFF9BE9A8)
                            count < 15 -> Color(0xFF40C463)
                            else -> Color(0xFF30A14E)
                        }
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    title: String, 
    supportingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
