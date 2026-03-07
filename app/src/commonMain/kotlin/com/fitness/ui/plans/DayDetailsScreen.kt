package com.fitness.ui.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.data.ExerciseProvider
import com.fitness.data.local.SetEntity
import com.fitness.util.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsScreen(
    date: String,
    sets: List<SetEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(date) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = getString("back"))
                    }
                }
            )
        }
    ) { padding ->
        if (sets.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(getString("history_empty"), color = Color.Gray)
            }
        } else {
            val groupedByExercise = sets.groupBy { it.exerciseName }
            
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedByExercise.toList()) { (exerciseId, exerciseSets) ->
                    val exercise = ExerciseProvider.exercises.find { it.id == exerciseId }
                    val name = exercise?.let { getString(it.nameKey) } ?: exerciseId
                    val isBodyweight = exercise?.isBodyweight ?: false
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            exercise?.let {
                                Text(
                                    text = getString(it.targetMuscleKey),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            exerciseSets.forEach { set ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val text = if (isBodyweight) {
                                        "${set.reps} ${getString("reps")}"
                                    } else {
                                        "${set.weight} ${getString("kg")}  x  ${set.reps}"
                                    }
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = set.timeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
