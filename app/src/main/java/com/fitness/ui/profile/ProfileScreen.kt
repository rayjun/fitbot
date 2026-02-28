package com.fitness.ui.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel, 
    settingsViewModel: SettingsViewModel,
    onLogout: () -> Unit
) {
    val heatmapData by viewModel.heatmapData.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val language by settingsViewModel.language.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 用户信息
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("R", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Ray Jun", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.quote), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.heatmap_title), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // 热力图组件
            WorkoutHeatMap(heatmapData)

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_general), fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(
                icon = Icons.Default.Settings, 
                title = stringResource(R.string.settings_dark_mode), 
                isSwitch = true,
                checked = isDarkMode
            ) {
                settingsViewModel.toggleDarkMode(!isDarkMode)
            }

            SettingsItem(
                icon = Icons.Default.Language, 
                title = stringResource(R.string.settings_language), 
                isSwitch = false,
                supportingText = if (language == "zh") "简体中文" else "English"
            ) {
                showLanguageDialog = true
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
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
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("取消")
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
    isSwitch: Boolean, 
    checked: Boolean = false,
    supportingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            if (isSwitch) {
                Switch(checked = checked, onCheckedChange = { _ -> onClick() })
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
