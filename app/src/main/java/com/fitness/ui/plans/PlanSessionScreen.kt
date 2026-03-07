package com.fitness.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.R
import com.fitness.data.ExerciseProvider
import com.fitness.model.Exercise
import com.fitness.ui.workout.WorkoutViewModel
import com.fitness.util.toResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSessionScreen(
    dayOfWeek: Int,
    planViewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onExerciseClick: (Exercise) -> Unit,
    onBack: () -> Unit
) {
    val currentPlan by planViewModel.currentPlan.collectAsStateWithLifecycle()
    val currentRoutine by planViewModel.currentRoutine.collectAsStateWithLifecycle()
    val setsToday by workoutViewModel.setsToday.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val plannedExercises = remember(currentRoutine, dayOfWeek) {
        currentRoutine.find { it.dayOfWeek == dayOfWeek }?.exercises ?: emptyList()
    }

    Scaffold(
        topBar = {
            val titleText = currentPlan?.name?.let { 
                if (it == "Daily Routine" || it == "Weekly Routine") {
                    stringResource(R.string.weekly_routine_name)
                } else it
            } ?: stringResource(R.string.training_session)

            TopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(stringResource(com.fitness.R.string.session_progress), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(plannedExercises) { planned ->
                    val exercise = ExerciseProvider.exercises.find { it.id == planned.id }
                    if (exercise != null) {
                        val completedCount = setsToday.count { it.exerciseName == exercise.id }
                        val isFinished = completedCount >= planned.targetSets
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onExerciseClick(exercise) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFinished) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                    else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        stringResource(exercise.nameKey.toResId(context)), 
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    ) 
                                },
                                supportingContent = { 
                                    Text("${stringResource(exercise.targetMuscleKey.toResId(context))} • $completedCount / ${planned.targetSets}") 
                                },
                                trailingContent = {
                                    Icon(
                                        Icons.Default.CheckCircle, 
                                        contentDescription = null,
                                        tint = if (isFinished) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
