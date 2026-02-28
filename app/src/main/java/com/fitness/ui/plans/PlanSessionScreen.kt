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
import com.fitness.ui.workout.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSessionScreen(
    planId: Int,
    planViewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onExerciseClick: (Exercise) -> Unit,
    onBack: () -> Unit
) {
    val currentPlan by planViewModel.currentPlan.collectAsStateWithLifecycle()
    val completedExercises by workoutViewModel.completedExercises.collectAsStateWithLifecycle(emptyList())
    
    // 进入此页面自动启动新训练会话
    LaunchedEffect(Unit) {
        workoutViewModel.startNewSession()
    }

    // 每次从 Workout 界面返回，都刷新一下 session 数据
    LaunchedEffect(Unit) {
        workoutViewModel.refreshSets()
    }

    val exercises = remember(currentPlan) {
        val ids = currentPlan?.exercisesJson?.split(",") ?: emptyList()
        ids.mapNotNull { id -> ExerciseProvider.exercises.find { it.id == id.trim() } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentPlan?.name ?: "Training Session") },
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
                    val isCompleted = completedExercises.contains(exercise.name)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onExerciseClick(exercise) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        ListItem(
                            headlineContent = { 
                                Text(
                                    exercise.name, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            supportingContent = { Text(exercise.targetMuscle) },
                            trailingContent = {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = null,
                                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                                )
                            }
                        )
                    }
                }
            }
            
            Button(
                onClick = {
                    workoutViewModel.finishWorkout()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Finish Session")
            }
        }
    }
}
