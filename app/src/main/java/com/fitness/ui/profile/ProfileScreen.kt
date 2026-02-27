package com.fitness.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onLogout: () -> Unit) {
    val heatmapData by viewModel.heatmapData.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("个人中心") }) }
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
                        Text("坚持就是胜利", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("训练热力图 (近 90 天)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // 热力图组件
            WorkoutHeatMap(heatmapData)

            Spacer(modifier = Modifier.height(32.dp))
            Text("通用设置", fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(Icons.Default.Settings, "深色模式", true) { /* TODO */ }
            SettingsItem(Icons.Default.Language, "语言 (Language)", false) { /* TODO */ }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            }
        }
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
        // 每 7 天一列 (模拟 GitHub 布局)
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
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, isSwitch: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            if (isSwitch) {
                var checked by remember { mutableStateOf(false) }
                Switch(checked = checked, onCheckedChange = { checked = it })
            }
        },
        modifier = androidx.compose.ui.Modifier.clickable { onClick() }
    )
}
