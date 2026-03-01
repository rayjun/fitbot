package com.fitness.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.R
import com.fitness.data.ExerciseProvider
import com.fitness.model.RoutineDay
import com.fitness.ui.workout.WorkoutViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    viewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onStartPlan: (Int) -> Unit
) {
    val currentRoutine by viewModel.currentRoutine.collectAsStateWithLifecycle()
    val allPlans by viewModel.allPlans.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }

    val todayOfWeek = LocalDate.now().dayOfWeek.value // 1 (Mon) - 7 (Sun)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (showHistory) R.string.plan_history else R.string.current_plan)) },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.plan_history))
                    }
                }
            )
        }
    ) { padding ->
        if (showHistory) {
            HistoryList(allPlans, padding)
        } else {
            CurrentPlanView(
                routine = currentRoutine, 
                todayOfWeek = todayOfWeek,
                padding = padding,
                viewModel = viewModel,
                workoutViewModel = workoutViewModel,
                onStartPlan = onStartPlan
            )
        }
    }
}

@Composable
fun CurrentPlanView(
    routine: List<RoutineDay>, 
    todayOfWeek: Int,
    padding: PaddingValues,
    viewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onStartPlan: (Int) -> Unit
) {
    var editingDay by remember { mutableStateOf<RoutineDay?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (routine.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(R.string.no_plan), color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val defaultRoutine = (1..7).map { day ->
                    RoutineDay(day, isRest = (day == 3 || day == 7), exercises = emptyList())
                }
                viewModel.updatePlan("Weekly Routine", defaultRoutine)
            }) {
                Text(stringResource(R.string.create_routine))
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Weekly Progress
            WeeklyProgressBar(routine, workoutViewModel, todayOfWeek)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's Task
            val todayRoutine = routine.find { it.dayOfWeek == todayOfWeek }
            TodayTaskCard(todayRoutine, todayOfWeek, onStartPlan)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.week_overview), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Overview List
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(routine.sortedBy { it.dayOfWeek }) { day ->
                    ListItem(
                        headlineContent = { Text(getDayName(day.dayOfWeek)) },
                        supportingContent = { 
                            Text(if (day.isRest) stringResource(R.string.rest_badge) else stringResource(R.string.exercises_count, day.exercises.size)) 
                        },
                        trailingContent = {
                            if (day.dayOfWeek == todayOfWeek) {
                                SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.today_badge)) })
                            }
                        },
                        modifier = Modifier.clickable { editingDay = day }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (editingDay != null) {
        EditDayDialog(
            day = editingDay!!,
            onDismiss = { editingDay = null },
            onSave = { isRest, exercises ->
                viewModel.updatePlanDay(editingDay!!.dayOfWeek, isRest, exercises)
                editingDay = null
            }
        )
    }
}

@Composable
fun EditDayDialog(
    day: RoutineDay,
    onDismiss: () -> Unit,
    onSave: (Boolean, List<String>) -> Unit
) {
    var isRest by remember { mutableStateOf(day.isRest) }
    val selectedExercises = remember { mutableStateListOf(*day.exercises.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_day) + " - " + getDayName(day.dayOfWeek)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(stringResource(R.string.is_rest_day), modifier = Modifier.weight(1f))
                    Switch(checked = isRest, onCheckedChange = { isRest = it })
                }
                
                if (!isRest) {
                    Text(stringResource(R.string.select_exercises), fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(ExerciseProvider.exercises) { exercise ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedExercises.contains(exercise.id)) {
                                            selectedExercises.remove(exercise.id)
                                        } else {
                                            selectedExercises.add(exercise.id)
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedExercises.contains(exercise.id),
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(exercise.nameRes))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(isRest, selectedExercises.toList()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun WeeklyProgressBar(routine: List<RoutineDay>, workoutViewModel: WorkoutViewModel, todayOfWeek: Int) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        routine.sortedBy { it.dayOfWeek }.forEach { day ->
            var isCompleted by remember { mutableStateOf(false) }
            
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            LaunchedEffect(day) {
                if (!day.isRest) {
                    val diff = day.dayOfWeek - todayOfWeek
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, diff)
                    val dateStr = dateFormatter.format(cal.time)
                    isCompleted = workoutViewModel.hasCompletedExercisesOnDate(dateStr)
                }
            }

            val color = when {
                day.isRest -> Color.Transparent
                isCompleted || day.dayOfWeek == todayOfWeek -> MaterialTheme.colorScheme.primary
                else -> Color.LightGray
            }
            val borderColor = if (day.isRest) Color.Green else Color.Transparent

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape)
                    .border(2.dp, borderColor, CircleShape)
            ) {
                Text(
                    text = days[day.dayOfWeek - 1],
                    color = if (color == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TodayTaskCard(todayRoutine: RoutineDay?, todayOfWeek: Int, onStartPlan: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.today_task), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (todayRoutine == null || todayRoutine.isRest) {
                Text(stringResource(R.string.rest_message), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(stringResource(R.string.exercises_scheduled, todayRoutine.exercises.size), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onStartPlan(todayOfWeek) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.start_training))
                }
            }
        }
    }
}

@Composable
fun HistoryList(plans: List<com.fitness.data.local.PlanEntity>, padding: PaddingValues) {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
        items(plans) { plan ->
            ListItem(
                headlineContent = { Text(plan.name) },
                supportingContent = { Text(stringResource(R.string.plan_created_at, df.format(Date(plan.createdAt)))) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun getDayName(day: Int): String {
    return when(day) {
        1 -> stringResource(R.string.day_mon)
        2 -> stringResource(R.string.day_tue)
        3 -> stringResource(R.string.day_wed)
        4 -> stringResource(R.string.day_thu)
        5 -> stringResource(R.string.day_fri)
        6 -> stringResource(R.string.day_sat)
        7 -> stringResource(R.string.day_sun)
        else -> stringResource(R.string.day_unknown)
    }
}
