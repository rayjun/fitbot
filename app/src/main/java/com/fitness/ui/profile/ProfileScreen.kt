package com.fitness.ui.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
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
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val language by settingsViewModel.language.collectAsStateWithLifecycle()
    val userQuote by settingsViewModel.userQuote.collectAsStateWithLifecycle()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQuoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_profile)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // User Info Card
            Card(modifier = Modifier.fillMaxWidth()) {
                if (account == null) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onLoginClick) {
                            Text("Login to Google Drive")
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
                                Icon(Icons.Default.Edit, contentDescription = "Edit Quote", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Workout Heatmap (90 days)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Heatmap Component (Filled width layout)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                WorkoutHeatMap(heatmapData)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("General Settings", fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(
                icon = Icons.Default.Settings, 
                title = "Theme Mode", 
                supportingText = when (themeMode) {
                    "dark" -> "Dark"
                    "light" -> "Light"
                    else -> "System Default"
                }
            ) {
                showThemeDialog = true
            }

            SettingsItem(
                icon = Icons.Default.Language, 
                title = "Language", 
                supportingText = if (language == "zh") "简体中文" else "English"
            ) {
                showLanguageDialog = true
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (account != null) {
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
                    Text("Logout")
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("简体中文") },
                        modifier = Modifier.clickable {
                            settingsViewModel.setLanguage("zh")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh"))
                            showLanguageDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("English") },
                        modifier = Modifier.clickable {
                            settingsViewModel.setLanguage("en")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme Mode") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("System Default") },
                        modifier = Modifier.clickable {
                            settingsViewModel.setThemeMode("system")
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Light") },
                        modifier = Modifier.clickable {
                            settingsViewModel.setThemeMode("light")
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Dark") },
                        modifier = Modifier.clickable {
                            settingsViewModel.setThemeMode("dark")
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showQuoteDialog) {
        var tempQuote by remember { mutableStateOf(userQuote) }
        AlertDialog(
            onDismissRequest = { showQuoteDialog = false },
            title = { Text("Edit Quote") },
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
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showQuoteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun WorkoutHeatMap(data: Map<String, Int>) {
    val days = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        for (i in 0..89) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list.reversed()
    }
    
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    LazyRow(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        val chunks = days.chunked(7)
        items(chunks) { week ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val dateStr = df.format(day)
                    val count = data[dateStr] ?: 0
                    val color = when {
                        count == 0 -> Color.LightGray.copy(alpha = 0.3f)
                        count < 5 -> Color(0xFFC6E48B)
                        count < 15 -> Color(0xFF7BC96F)
                        else -> Color(0xFF239A3B)
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
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
        modifier = Modifier.clickable { onClick() }
    )
}
