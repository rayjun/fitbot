package com.fitness.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
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
import com.fitness.ui.components.ExerciseImage
import com.fitness.util.getString
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userQuote: String,
    heatmapData: Map<String, Int>,
    accountName: String?,
    accountPhotoUrl: String?,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpdateQuote: (String) -> Unit
) {
    var showQuoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            TopAppBar(
                title = { 
                    Text(
                        getString("nav_profile"), 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                if (accountName == null) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AccountCircle, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp), 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onLoginClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(getString("login_drive"), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Profile Image with Placeholder
                        Box(modifier = Modifier.size(64.dp)) {
                            if (accountPhotoUrl != null) {
                                // We use a shared ExerciseImage component or similar for KMP image loading
                                // But for Profile Photo which is a URL, we need a URL-capable loader.
                                // For now, we'll try to use a placeholder or common loader if available.
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            accountName.firstOrNull()?.toString() ?: "U", 
                                            fontSize = 24.sp, 
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            accountName.firstOrNull()?.toString() ?: "U", 
                                            fontSize = 24.sp, 
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showQuoteDialog = true }.padding(top = 4.dp)
                            ) {
                                Text(
                                    userQuote, 
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Edit, 
                                    contentDescription = getString("edit_quote"), 
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                getString("heatmap_title"), 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Heatmap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(12.dp), 
                contentAlignment = Alignment.Center
            ) {
                WorkoutHeatMap(heatmapData)
            }

            Spacer(modifier = Modifier.weight(1f))
            
            if (accountName != null) {
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(getString("logout"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showQuoteDialog) {
        var tempQuote by remember { mutableStateOf(userQuote) }
        AlertDialog(
            onDismissRequest = { showQuoteDialog = false },
            title = { Text(getString("edit_quote"), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempQuote,
                    onValueChange = { tempQuote = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    onUpdateQuote(tempQuote)
                    showQuoteDialog = false 
                }) { Text(getString("save"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showQuoteDialog = false }) { Text(getString("dialog_cancel")) }
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
