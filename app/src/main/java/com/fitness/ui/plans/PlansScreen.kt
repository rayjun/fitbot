package com.fitness.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
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
import com.fitness.model.PlannedExercise
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
    val allHistorySets by workoutViewModel.allHistorySets.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }

    val todayOfWeek = LocalDate.now().dayOfWeek.value // 1 (Mon) - 7 (Sun)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (showHistory) R.string.history_title else R.string.current_plan)) },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            imageVector = Icons.Default.History, 
                            contentDescription = stringResource(R.string.history_title),
                            tint = if (showHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (showHistory) {
            WorkoutHistoryList(allHistorySets, padding)
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
fun WorkoutHistoryList(allSets: List<com.fitness.data.local.SetEntity>, padding: PaddingValues) {
    if (allSets.isEmpty()) {
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.history_empty), color = Color.Gray)
        }
    } else {
        val grouped = allSets.groupBy { it.date }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (date, dailySets) ->
                item {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                val exercisesInDay = dailySets.groupBy { it.exerciseName }
                items(exercisesInDay.toList()) { (exerciseId, exerciseSets) ->
                    val exercise = ExerciseProvider.exercises.find { it.id == exerciseId }
                    val name = exercise?.let { stringResource(it.nameRes) } ?: exerciseId
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        ListItem(
                            headlineContent = { Text(name, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                Text(exerciseSets.joinToString(" | ") { "${it.weight}kg x ${it.reps}" })
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }
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
    val setsToday by workoutViewModel.setsToday.collectAsStateWithLifecycle()

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
            val isCompleted = remember(setsToday, todayRoutine) {
                if (todayRoutine == null || todayRoutine.isRest) false
                else todayRoutine.exercises.isNotEmpty() && todayRoutine.exercises.all { planned ->
                    val count = setsToday.count { it.exerciseName == planned.id }
                    count >= planned.targetSets
                }
            }

            TodayTaskCard(todayRoutine, todayOfWeek, isCompleted, onStartPlan)
            
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
    onSave: (Boolean, List<PlannedExercise>) -> Unit
) {
    var isRest by remember { mutableStateOf(day.isRest) }
    // Map of exercise ID to targetSets
    val selectedMap = remember { 
        mutableStateMapOf<String, Int>().apply {
            day.exercises.forEach { put(it.id, it.targetSets) }
        }
    }

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
                            val currentSets = selectedMap[exercise.id]
                            val isChecked = currentSets != null
                            
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                selectedMap.remove(exercise.id)
                                            } else {
                                                selectedMap[exercise.id] = 3
                                            }
                                        }
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(exercise.nameRes), modifier = Modifier.weight(1f))
                                    
                                    if (isChecked) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { 
                                                    val s = selectedMap[exercise.id] ?: 3
                                                    if (s > 1) selectedMap[exercise.id] = s - 1
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                                            }
                                            Text(text = "${selectedMap[exercise.id]}", modifier = Modifier.padding(horizontal = 4.dp))
                                            IconButton(
                                                onClick = { 
                                                    val s = selectedMap[exercise.id] ?: 3
                                                    selectedMap[exercise.id] = s + 1
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                val list = selectedMap.map { PlannedExercise(it.key, it.value) }
                onSave(isRest, list) 
            }) {
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
            var isFullyCompleted by remember { mutableStateOf(false) }
            
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            LaunchedEffect(day, routine) {
                val diff = day.dayOfWeek - todayOfWeek
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, diff)
                val dateStr = dateFormatter.format(cal.time)
                
                isFullyCompleted = if (day.isRest) {
                    true // 休息日默认视为“任务已完成”
                } else {
                    workoutViewModel.isDayFullyCompleted(dateStr, day.exercises)
                }
            }

            val color = when {
                day.isRest -> Color.Transparent
                isFullyCompleted -> MaterialTheme.colorScheme.primary
                day.dayOfWeek == todayOfWeek -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // 今天正在进行中，显示浅橙
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val borderColor = if (day.isRest) MaterialTheme.colorScheme.outline else Color.Transparent
            val borderWidth = if (day.isRest) 1.dp else 0.dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape)
                    .border(borderWidth, borderColor, CircleShape)
            ) {
                Text(
                    text = days[day.dayOfWeek - 1],
                    color = if (isFullyCompleted && !day.isRest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TodayTaskCard(
    todayRoutine: RoutineDay?, 
    todayOfWeek: Int, 
    isCompleted: Boolean,
    onStartPlan: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.today_task), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (todayRoutine == null || todayRoutine.isRest) {
                Text(stringResource(R.string.rest_message), style = MaterialTheme.typography.bodyLarge)
            } else if (isCompleted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.training_completed), 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onStartPlan(todayOfWeek) }) {
                    Text("继续训练 (Continue)")
                }
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
