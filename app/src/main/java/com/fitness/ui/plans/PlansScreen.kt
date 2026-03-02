package com.fitness.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    viewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onStartPlan: (Int) -> Unit,
    onDayClick: (String) -> Unit
) {
    val currentRoutine by viewModel.currentRoutine.collectAsStateWithLifecycle()
    var weekOffset by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_plans)) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            WeekSelector(
                weekOffset = weekOffset,
                onOffsetChange = { weekOffset = it }
            )
            
            CurrentPlanView(
                routine = currentRoutine, 
                weekOffset = weekOffset,
                padding = PaddingValues(0.dp),
                viewModel = viewModel,
                workoutViewModel = workoutViewModel,
                onStartPlan = onStartPlan,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
fun WeekSelector(weekOffset: Int, onOffsetChange: (Int) -> Unit) {
    val today = LocalDate.now()
    val mondayOfSelectedWeek = today
        .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        .plusWeeks(weekOffset.toLong())
    val sundayOfSelectedWeek = mondayOfSelectedWeek.plusDays(6)
    
    val formatter = DateTimeFormatter.ofPattern("MM月dd日")
    val rangeText = if (weekOffset == 0) {
        stringResource(R.string.current_tag) + " (${mondayOfSelectedWeek.format(formatter)} - ${sundayOfSelectedWeek.format(formatter)})"
    } else {
        "${mondayOfSelectedWeek.format(formatter)} - ${sundayOfSelectedWeek.format(formatter)}"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onOffsetChange(weekOffset - 1) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Week")
        }
        Text(text = rangeText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onOffsetChange(weekOffset + 1) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Week")
        }
    }
}

@Composable
fun CurrentPlanView(
    routine: List<RoutineDay>, 
    weekOffset: Int,
    padding: PaddingValues,
    viewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onStartPlan: (Int) -> Unit,
    onDayClick: (String) -> Unit
) {
    var editingDayOfWeek by remember { mutableIntStateOf(-1) }
    val setsToday by workoutViewModel.setsToday.collectAsStateWithLifecycle()
    
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek.value

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
            // Weekly Progress Bar
            WeeklyProgressBar(
                routine = routine, 
                workoutViewModel = workoutViewModel, 
                weekOffset = weekOffset,
                onDayClick = onDayClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's Action Card (Only if we are in current week)
            if (weekOffset == 0) {
                val todayRoutine = routine.find { it.dayOfWeek == todayDayOfWeek }
                val isCompleted = remember(setsToday, todayRoutine) {
                    if (todayRoutine == null || todayRoutine.isRest) false
                    else todayRoutine.exercises.isNotEmpty() && todayRoutine.exercises.all { planned ->
                        val count = setsToday.count { it.exerciseName == planned.id }
                        count >= planned.targetSets
                    }
                }
                
                TodayTaskCard(todayRoutine, todayDayOfWeek, isCompleted, onStartPlan)
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Text(stringResource(R.string.week_overview), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Overview List
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(routine.sortedBy { it.dayOfWeek }) { day ->
                    val dateStr = remember(day.dayOfWeek, weekOffset) {
                        val monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        monday.plusWeeks(weekOffset.toLong()).plusDays((day.dayOfWeek - 1).toLong())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }

                    ListItem(
                        headlineContent = { Text(getDayName(day.dayOfWeek)) },
                        supportingContent = { 
                            Text(if (day.isRest) stringResource(R.string.rest_badge) else stringResource(R.string.exercises_count, day.exercises.size)) 
                        },
                        trailingContent = {
                            if (weekOffset == 0 && day.dayOfWeek == todayDayOfWeek) {
                                SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.today_badge)) })
                            }
                        },
                        modifier = Modifier.clickable { 
                            if (weekOffset == 0) editingDayOfWeek = day.dayOfWeek 
                            else onDayClick(dateStr)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (editingDayOfWeek != -1) {
        val dayToEdit = routine.find { it.dayOfWeek == editingDayOfWeek }
        if (dayToEdit != null) {
            EditDayDialog(
                day = dayToEdit,
                onDismiss = { editingDayOfWeek = -1 },
                onSave = { isRest, exercises ->
                    viewModel.updatePlanDay(editingDayOfWeek, isRest, exercises)
                    editingDayOfWeek = -1
                }
            )
        }
    }
}

@Composable
fun WeeklyProgressBar(
    routine: List<RoutineDay>, 
    workoutViewModel: WorkoutViewModel, 
    weekOffset: Int,
    onDayClick: (String) -> Unit
) {
    val days = listOf("一", "二", "三", "四", "五", "六", "日")
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek.value

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        routine.sortedBy { it.dayOfWeek }.forEach { day ->
            var isFullyCompleted by remember { mutableStateOf(false) }
            
            val circleDate = remember(day.dayOfWeek, weekOffset) {
                val monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                monday.plusWeeks(weekOffset.toLong()).plusDays((day.dayOfWeek - 1).toLong())
            }
            val circleDateStr = circleDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            LaunchedEffect(circleDateStr, routine) {
                isFullyCompleted = if (day.isRest) {
                    true 
                } else {
                    workoutViewModel.isDayFullyCompleted(circleDateStr, day.exercises)
                }
            }

            val isActualToday = weekOffset == 0 && day.dayOfWeek == todayDayOfWeek

            val color = when {
                day.isRest -> Color.Transparent
                isFullyCompleted -> MaterialTheme.colorScheme.primary
                isActualToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val borderColor = if (day.isRest) MaterialTheme.colorScheme.outline else Color.Transparent

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, borderColor, CircleShape)
                    .clickable { onDayClick(circleDateStr) }
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
fun EditDayDialog(
    day: RoutineDay,
    onDismiss: () -> Unit,
    onSave: (Boolean, List<PlannedExercise>) -> Unit
) {
    var isRest by remember { mutableStateOf(day.isRest) }
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
                                    Checkbox(checked = isChecked, onCheckedChange = null)
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
                                            ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                                            Text(text = "${selectedMap[exercise.id]}", modifier = Modifier.padding(horizontal = 4.dp))
                                            IconButton(
                                                onClick = { 
                                                    val s = selectedMap[exercise.id] ?: 3
                                                    selectedMap[exercise.id] = s + 1
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
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
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
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
