package com.fitness.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.ui.components.CompactTopAppBar
import com.fitness.ui.components.RadarChart
import com.fitness.ui.components.VolumeBarChart
import com.fitness.ui.components.AnatomyMap
import com.fitness.util.getString
import com.fitness.util.getLocalizedTypedString
import com.fitness.util.LocalAppLanguage

@Composable
fun AnalyticsScreen(
    muscleVolumeData: Map<String, Double>,
    selectedCategory: String?,
    selectedTimeRange: TimeRange,
    onCategoryClick: (String?) -> Unit,
    onTimeRangeClick: (TimeRange) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val currentLang = LocalAppLanguage.current
    var isBackView by remember { mutableStateOf(false) }
    
    // Prepare translated labels for muscle categories
    val categories = listOf("cat_chest", "cat_back", "cat_legs", "cat_arms", "cat_shoulders", "cat_core", "cat_full_body")
    val labels = remember(currentLang) {
        categories.associateWith { getLocalizedTypedString(it, currentLang) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CompactTopAppBar(
                title = getString("analytics_title"),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Time Range Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeRange.entries.forEach { range ->
                    val isSelected = range == selectedTimeRange
                    val labelKey = when(range) {
                        TimeRange.WEEK -> "range_week"
                        TimeRange.MONTH -> "range_month"
                        TimeRange.YEAR -> "range_year"
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onTimeRangeClick(range) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            getString(labelKey),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Category Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    getString("detail_target_muscle"), // Using an existing string like "Target Muscle"
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategory == null) 0 else categories.indexOf(selectedCategory) + 1,
                edgePadding = 8.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedCategory == null,
                    onClick = { onCategoryClick(null) },
                    text = { Text(getString("category_all"), style = MaterialTheme.typography.labelMedium) }
                )
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryClick(cat) },
                        text = { Text(labels[cat] ?: cat, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Dynamic Title based on selection
            val chartSuffix = if (selectedCategory != null) " - ${labels[selectedCategory]}" else ""

            // 3. Muscle Balance (Radar)
            Text(
                getString("analytics_radar_title") + chartSuffix,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                RadarChart(
                    data = muscleVolumeData,
                    labels = labels
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Total Volume (Bar)
            Text(
                getString("analytics_volume_title") + chartSuffix,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                VolumeBarChart(
                    data = muscleVolumeData,
                    labels = labels
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
