package com.fitness.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.auth.UserProfile
import com.fitness.ui.components.RemoteImage
import com.fitness.util.getString
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    userQuote: String,
    heatmapData: Map<String, Int>,
    onLoginClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onAiCoachClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditQuote: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var showEditQuoteDialog by remember { mutableStateOf(false) }
    var currentQuoteText by remember { mutableStateOf(userQuote) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. User Info Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.photoUrl != null) {
                    RemoteImage(
                        url = userProfile.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (userProfile?.name != null) {
                    Text(
                        text = userProfile.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { 
                        currentQuoteText = userQuote
                        showEditQuoteDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (userProfile == null) {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(getString("login_drive") ?: "Login", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 2. Training Heatmap Card
        Text(
            getString("heatmap_title") ?: "Workout Heatmap",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                WorkoutHeatMap(data = heatmapData)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Menu List Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column {
                // Analytics
                ListItem(
                    headlineContent = { 
                        Text(
                            getString("analytics_title") ?: "Analytics", 
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    leadingContent = { 
                        Icon(
                            Icons.Default.BarChart, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    trailingContent = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.clickable { onAnalyticsClick() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp), 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // AI Coach
                ListItem(
                    headlineContent = { 
                        Text(
                            getString("ai_coach_title") ?: "AI Coach", 
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    leadingContent = { 
                        Icon(
                            Icons.Default.Psychology, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    trailingContent = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.clickable { onAiCoachClick() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp), 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Settings
                ListItem(
                    headlineContent = { 
                        Text(
                            getString("settings_title") ?: "Settings", 
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    leadingContent = { 
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    trailingContent = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.clickable { onSettingsClick() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }

    if (showEditQuoteDialog) {
        AlertDialog(
            onDismissRequest = { showEditQuoteDialog = false },
            title = { Text(getString("edit_quote") ?: "Edit Quote") },
            text = {
                OutlinedTextField(
                    value = currentQuoteText,
                    onValueChange = { currentQuoteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    onEditQuote(currentQuoteText)
                    showEditQuoteDialog = false 
                }) {
                    Text(getString("save") ?: "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditQuoteDialog = false }) {
                    Text(getString("dialog_cancel") ?: "Cancel")
                }
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
        val columns = (maxWidth / columnWidth).toInt().coerceAtLeast(1)
        val totalDays = columns * 7

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        val days = remember(totalDays, today) {
            (0 until totalDays).map { i ->
                today.minus(i, DateTimeUnit.DAY)
            }.reversed()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weeks = days.chunked(7)
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                    week.forEach { day ->
                        val dateStr = day.toString() // YYYY-MM-DD
                        val count = data[dateStr] ?: 0
                        val color = when {
                            count == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            count < 5 -> Color(0xFF40C463) 
                            count < 15 -> Color(0xFF30A14E)
                            else -> Color(0xFF216E39) 
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
