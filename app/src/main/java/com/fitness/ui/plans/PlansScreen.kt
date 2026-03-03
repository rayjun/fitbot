package com.fitness.ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlansScreen(
    viewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onStartPlan: (Int) -> Unit,
    onDayClick: (String) -> Unit
) {
    val currentRoutine by viewModel.currentRoutine.collectAsStateWithLifecycle()
    
    val initialPage = 500
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = initialPage)
    val weekOffset = pagerState.currentPage - initialPage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_plans), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            WeekSelectorHeader(weekOffset = weekOffset)
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { page ->
                val currentWeekOffset = page - initialPage
                CurrentPlanView(
                    routine = currentRoutine, 
                    weekOffset = currentWeekOffset,
                    padding = PaddingValues(0.dp),
                    viewModel = viewModel,
                    workoutViewModel = workoutViewModel,
                    onStartPlan = onStartPlan,
                    onDayClick = onDayClick
                )
            }
        }
    }
}

@Composable
fun WeekSelectorHeader(weekOffset: Int) {
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

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = null, 
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = rangeText, 
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward, 
                contentDescription = null, 
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (routine.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxHeight(0.8f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.no_plan), color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val defaultRoutine = (1..7).map { day ->
                                RoutineDay(day, isRest = (day == 3 || day == 7), exercises = emptyList())
                            }
                            viewModel.updatePlan("Daily Routine", defaultRoutine)
                        }) {
                            Text(stringResource(R.string.create_routine))
                        }
                    }
                }
            }
        } else {
            item {
                WeeklyProgressBar(
                    routine = routine, 
                    workoutViewModel = workoutViewModel, 
                    weekOffset = weekOffset,
                    onDayClick = onDayClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            if (weekOffset == 0) {
                item {
                    val todayRoutine = routine.find { it.dayOfWeek == todayDayOfWeek }
                    val isCompleted = remember(setsToday, todayRoutine) {
                        if (todayRoutine == null || todayRoutine.isRest) false
                        else todayRoutine.exercises.isNotEmpty() && todayRoutine.exercises.all { planned ->
                            val count = setsToday.count { it.exerciseName == planned.id }
                            count >= planned.targetSets
                        }
                    }
                    
                    TodayTaskCard(todayRoutine, todayDayOfWeek, isCompleted, onStartPlan)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            item {
                Text(
                    stringResource(R.string.week_overview), 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
            
            items(routine.sortedBy { it.dayOfWeek }) { day ->
                val dateStr = remember(day.dayOfWeek, weekOffset) {
                    val monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    monday.plusWeeks(weekOffset.toLong()).plusDays((day.dayOfWeek - 1).toLong())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { 
                            if (weekOffset == 0) editingDayOfWeek = day.dayOfWeek 
                            else onDayClick(dateStr)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (weekOffset == 0 && day.dayOfWeek == todayDayOfWeek)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    ListItem(
                        headlineContent = { 
                            Text(
                                getDayName(day.dayOfWeek),
                                fontWeight = if (weekOffset == 0 && day.dayOfWeek == todayDayOfWeek) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        supportingContent = { 
                            Text(if (day.isRest) stringResource(R.string.rest_badge) else stringResource(R.string.exercises_count, day.exercises.size)) 
                        },
                        trailingContent = {
                            if (weekOffset == 0 && day.dayOfWeek == todayDayOfWeek) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(stringResource(R.string.today_badge), modifier = Modifier.padding(4.dp))
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
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
                isFullyCompleted = if (day.isRest) true 
                else workoutViewModel.isDayFullyCompleted(circleDateStr, day.exercises)
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, borderColor, CircleShape)
                    .clickable { onDayClick(circleDateStr) }
            ) {
                Text(
                    text = days[day.dayOfWeek - 1],
                    color = if (isFullyCompleted && !day.isRest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isCompleted) 
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f) 
                else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.today_task), 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isCompleted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (todayRoutine == null || todayRoutine.isRest) {
                Text(stringResource(R.string.rest_message), style = MaterialTheme.typography.bodyLarge)
            } else if (isCompleted) {
                Text(
                    stringResource(R.string.training_completed), 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onStartPlan(todayOfWeek) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("继续训练 (Continue)", color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                Text(
                    stringResource(R.string.exercises_scheduled, todayRoutine.exercises.size), 
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onStartPlan(todayOfWeek) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.start_training), fontWeight = FontWeight.Bold)
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
