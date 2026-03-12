package com.fitness.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val currentLang = LocalAppLanguage.current
    
    // Prepare translated labels for muscle categories correctly
    val categories = listOf("cat_chest", "cat_back", "cat_legs", "cat_arms", "cat_shoulders", "cat_core", "cat_full_body")
    val labels = remember(currentLang) {
        categories.associateWith { getLocalizedTypedString(it, currentLang) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CompactTopAppBar(
                title = getString("analytics_title") ?: "Data Analytics",
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
            
            // 1. Muscle Balance (Radar)
            Text(
                getString("analytics_radar_title") ?: "Muscle Balance",
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

            // 2. Anatomy Heatmap
            Text(
                getString("analytics_anatomy_title") ?: "Physique Focus",
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
                AnatomyMap(volumeData = muscleVolumeData)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Total Volume (Bar)
            Text(
                getString("analytics_volume_title") ?: "Total Volume",
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
