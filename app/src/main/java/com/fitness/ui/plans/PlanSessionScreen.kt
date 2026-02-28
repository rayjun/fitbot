package com.fitness.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.data.ExerciseProvider
import com.fitness.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSessionScreen(
    planId: Int,
    viewModel: PlanViewModel,
    onExerciseClick: (Exercise) -> Unit,
    onBack: () -> Unit
) {
    val currentPlan by viewModel.currentPlan.collectAsStateWithLifecycle()
    
    val exercises = remember(currentPlan) {
        val ids = currentPlan?.exercisesJson?.split(",") ?: emptyList()
        ids.mapNotNull { id -> ExerciseProvider.exercises.find { it.id == id.trim() } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentPlan?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Session Progress:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(exercises) { exercise ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onExerciseClick(exercise) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        ListItem(
                            headlineContent = { Text(exercise.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(exercise.targetMuscle) },
                            trailingContent = {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = null,
                                    tint = Color.Gray.copy(alpha = 0.3f) // TODO: track completion
                                )
                            }
                        )
                    }
                }
            }
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Finish Session")
            }
        }
    }
}
