package com.fitness.ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.DeleteOutline
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlansScreen(
    viewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onStartExercise: (String, String) -> Unit, // Updated to pass (exerciseId, date)
    onDayClick: (String) -> Unit
) {
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
                InteractivePlanView(
                    weekOffset = currentWeekOffset,
                    viewModel = viewModel,
                    workoutViewModel = workoutViewModel,
                    onStartExercise = onStartExercise,
                    onDayDetailsClick = onDayClick
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

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(text = rangeText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun InteractivePlanView(
    weekOffset: Int,
    viewModel: PlanViewModel,
    workoutViewModel: WorkoutViewModel,
    onStartExercise: (String, String) -> Unit,
    onDayDetailsClick: (String) -> Unit
) {
    val today = LocalDate.now()
    var selectedDayOfWeek by remember { mutableIntStateOf(if (weekOffset == 0) today.dayOfWeek.value else 1) }
    val currentRoutine by viewModel.currentRoutine.collectAsStateWithLifecycle()
    val setsToday by workoutViewModel.setsToday.collectAsStateWithLifecycle()

    val selectedDate = remember(selectedDayOfWeek, weekOffset) {
        today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            .plusWeeks(weekOffset.toLong()).plusDays((selectedDayOfWeek - 1).toLong())
    }
    val selectedDateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val endOfDayTimestamp = selectedDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    var dayRoutineSnapshot by remember { mutableStateOf<RoutineDay?>(null) }
    LaunchedEffect(endOfDayTimestamp, currentRoutine) {
        val fullRoutine = viewModel.getRoutineForTimestamp(endOfDayTimestamp)
        dayRoutineSnapshot = fullRoutine.find { it.dayOfWeek == selectedDayOfWeek }
    }

    val displayDay = dayRoutineSnapshot ?: RoutineDay(selectedDayOfWeek, true, emptyList())
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<PlannedExercise?>(null) }

    var recordedSetsForSelectedDay by remember { mutableStateOf<List<com.fitness.data.local.SetEntity>>(emptyList()) }
    LaunchedEffect(selectedDateStr, setsToday) {
        recordedSetsForSelectedDay = workoutViewModel.getSetsByDate(selectedDateStr)
    }

    val isTrainingAllowed = (weekOffset > 0) || (weekOffset == 0 && selectedDayOfWeek >= today.dayOfWeek.value)
    // 只有今天或未来的日期才允许修改计划（增加动作）
    val isEditingAllowed = (weekOffset > 0) || (weekOffset == 0 && selectedDayOfWeek >= today.dayOfWeek.value)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        WeeklyProgressBarNavigation(
            weekOffset = weekOffset,
            selectedDayOfWeek = selectedDayOfWeek,
            currentRoutine = currentRoutine,
            workoutViewModel = workoutViewModel,
            onDaySelect = { selectedDayOfWeek = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getDayName(selectedDayOfWeek) + " " + (if (weekOffset == 0 && selectedDayOfWeek == today.dayOfWeek.value) " (今天)" else ""),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            TextButton(onClick = { onDayDetailsClick(selectedDateStr) }) {
                Text("查看历史详情", fontSize = 12.sp)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (displayDay.exercises.isEmpty() || displayDay.isRest) {
                item {
                    Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (displayDay.isRest) "今天是休息日" else "暂无动作安排", color = Color.Gray)
                    }
                }
            } else {
                items(displayDay.exercises) { planned ->
                    val exercise = ExerciseProvider.exercises.find { it.id == planned.id }
                    if (exercise != null) {
                        val completedCount = recordedSetsForSelectedDay.count { it.exerciseName == planned.id }
                        val isFinished = completedCount >= planned.targetSets

                        ExerciseActionCard(
                            exercise = exercise,
                            planned = planned,
                            completedCount = completedCount,
                            isFinished = isFinished,
                            isTrainingAllowed = isTrainingAllowed,
                            isEditable = (weekOffset == 0),
                            onStart = { if (isTrainingAllowed) onStartExercise(exercise.id, selectedDateStr) },
                            onDelete = { exerciseToDelete = planned }
                        )
                    }
                }
            }

            if (isEditingAllowed) {
                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("增加动作")
                    }
                }
            }
        }
    }

    if (exerciseToDelete != null) {
        val exercise = ExerciseProvider.exercises.find { it.id == exerciseToDelete!!.id }
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("确认删除动作") },
            text = { Text("确定要将“${exercise?.let { stringResource(it.nameRes) } ?: exerciseToDelete!!.id}”从当天的计划中移除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val updatedExercises = displayDay.exercises.filter { it.id != exerciseToDelete!!.id }
                    viewModel.updatePlanDay(selectedDayOfWeek, updatedExercises.isEmpty(), updatedExercises)
                    exerciseToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) { Text("取消") }
            }
        )
    }

    if (showAddExerciseDialog) {
        QuickAddExerciseDialog(
            currentExercises = displayDay.exercises,
            onDismiss = { showAddExerciseDialog = false },
            onSave = { selectedIds ->
                val newPlanned = selectedIds.map { id ->
                    displayDay.exercises.find { it.id == id } ?: PlannedExercise(id, 3)
                }
                viewModel.updatePlanDay(selectedDayOfWeek, newPlanned.isEmpty(), newPlanned)
                showAddExerciseDialog = false
            }
        )
    }
}

@Composable
fun WeeklyProgressBarNavigation(
    weekOffset: Int,
    selectedDayOfWeek: Int,
    currentRoutine: List<RoutineDay>,
    workoutViewModel: WorkoutViewModel,
    onDaySelect: (Int) -> Unit
) {
    val days = listOf("一", "二", "三", "四", "五", "六", "日")
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek.value

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        (1..7).forEach { dayNum ->
            val dayPlan = currentRoutine.find { it.dayOfWeek == dayNum }
            val isRest = dayPlan?.isRest ?: (dayNum == 3 || dayNum == 7)
            
            var isFullyCompleted by remember { mutableStateOf(false) }
            val circleDate = remember(dayNum, weekOffset) {
                today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .plusWeeks(weekOffset.toLong()).plusDays((dayNum - 1).toLong())
            }
            val circleDateStr = circleDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            LaunchedEffect(circleDateStr, currentRoutine) {
                isFullyCompleted = if (isRest) true 
                else workoutViewModel.isDayFullyCompleted(circleDateStr, dayPlan?.exercises ?: emptyList())
            }

            val isSelected = selectedDayOfWeek == dayNum
            val isActualToday = weekOffset == 0 && dayNum == todayDayOfWeek

            val color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isRest -> Color.Transparent
                isFullyCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                isActualToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else if (isRest) MaterialTheme.colorScheme.outline else Color.Transparent

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, borderColor, CircleShape)
                    .clickable { onDaySelect(dayNum) }
            ) {
                Text(
                    text = days[dayNum - 1],
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ExerciseActionCard(
    exercise: com.fitness.model.Exercise,
    planned: PlannedExercise,
    completedCount: Int,
    isFinished: Boolean,
    isTrainingAllowed: Boolean,
    isEditable: Boolean,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFinished) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable(enabled = isTrainingAllowed) { onStart() }
            ) {
                Text(
                    stringResource(exercise.nameRes), 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${stringResource(exercise.targetMuscleRes)} • $completedCount / ${planned.targetSets} 组",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isFinished) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
            } else if (isTrainingAllowed) {
                IconButton(onClick = onStart) {
                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (isEditable) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun QuickAddExerciseDialog(
    currentExercises: List<PlannedExercise>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>().apply { 
        addAll(currentExercises.map { it.id }) 
    } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理训练动作") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(ExerciseProvider.exercises) { exercise ->
                    val isSelected = selectedIds.contains(exercise.id)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            if (isSelected) selectedIds.remove(exercise.id) else selectedIds.add(exercise.id)
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(exercise.nameRes), fontWeight = FontWeight.Medium)
                            Text(stringResource(exercise.targetMuscleRes), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedIds.toList()) }) { Text("保存修改") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
fun getDayName(day: Int): String {
    return when(day) {
        1 -> stringResource(R.string.day_mon); 2 -> stringResource(R.string.day_tue); 3 -> stringResource(R.string.day_wed); 4 -> stringResource(R.string.day_thu); 5 -> stringResource(R.string.day_fri); 6 -> stringResource(R.string.day_sat); 7 -> stringResource(R.string.day_sun)
        else -> stringResource(R.string.day_unknown)
    }
}
