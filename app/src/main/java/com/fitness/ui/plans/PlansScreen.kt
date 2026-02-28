package com.fitness.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
        },
        floatingActionButton = {
            if (!showHistory) {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            val defaultRoutine = (1..7).map { day ->
                                if (day == 3 || day == 7) {
                                    RoutineDay(day, isRest = true, exercises = emptyList())
                                } else {
                                    RoutineDay(day, isRest = false, exercises = listOf("benchpress", "squat"))
                                }
                            }
                            viewModel.updatePlan("Weekly Full Body", defaultRoutine)
                        },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text(stringResource(R.string.update_plan)) }
                    )
                }
            }
        }
    ) { padding ->
        if (showHistory) {
            HistoryList(allPlans, padding)
        } else {
            CurrentPlanView(
                routine = currentRoutine, 
                todayOfWeek = todayOfWeek,
                padding = padding,
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
    workoutViewModel: WorkoutViewModel,
    onStartPlan: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (routine.isEmpty()) {
            Text(stringResource(R.string.no_plan), color = MaterialTheme.colorScheme.outline)
        } else {
            // Weekly Progress
            WeeklyProgressBar(routine, workoutViewModel, todayOfWeek)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's Task
            val todayRoutine = routine.find { it.dayOfWeek == todayOfWeek }
            TodayTaskCard(todayRoutine, todayOfWeek, onStartPlan)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("This Week's Overview", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Overview List
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(routine.sortedBy { it.dayOfWeek }) { day ->
                    ListItem(
                        headlineContent = { Text(getDayName(day.dayOfWeek)) },
                        supportingContent = { 
                            Text(if (day.isRest) "Rest" else "Exercises: ${day.exercises.size}") 
                        },
                        trailingContent = {
                            if (day.dayOfWeek == todayOfWeek) {
                                SuggestionChip(onClick = {}, label = { Text("Today") })
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
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
            Text("Today's Task", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (todayRoutine == null || todayRoutine.isRest) {
                Text("Rest day today! Enjoy your recovery.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("${todayRoutine.exercises.size} exercises scheduled.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onStartPlan(todayOfWeek) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Training")
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

fun getDayName(day: Int): String {
    return when(day) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Unknown"
    }
}
