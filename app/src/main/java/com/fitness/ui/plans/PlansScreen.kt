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
    onStartExercise: (String, String) -> Unit,
    onDayClick: (String) -> Unit
) {
    val initialPage = 500
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = initialPage)
    val weekOffset = pagerState.currentPage - initialPage

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.nav_plans), 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
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
    
    val formatter = DateTimeFormatter.ofPattern("MM.dd")
    val rangeText = "${mondayOfSelectedWeek.format(formatter)} - ${sundayOfSelectedWeek.format(formatter)}"

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (weekOffset == 0) {
                    Text(
                        text = stringResource(R.string.current_tag),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(text = rangeText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

    val isEditingAllowed = (weekOffset == 0)
    val isTrainingAllowed = (weekOffset > 0) || (weekOffset == 0 && selectedDayOfWeek >= today.dayOfWeek.value)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        WeeklyProgressBarNavigation(
            weekOffset = weekOffset,
            selectedDayOfWeek = selectedDayOfWeek,
            currentRoutine = currentRoutine,
            workoutViewModel = workoutViewModel,
            onDaySelect = { selectedDayOfWeek = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = getDayName(selectedDayOfWeek),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (weekOffset == 0 && selectedDayOfWeek == today.dayOfWeek.value) {
                    Text(
                        text = stringResource(R.string.current_tag),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TextButton(onClick = { onDayDetailsClick(selectedDateStr) }) {
                Text(stringResource(R.string.heatmap_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (displayDay.isRest) {
                item {
                    Box(modifier = Modifier.fillParentMaxHeight(0.4f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.day_unknown), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (isEditingAllowed) {
                                TextButton(onClick = { showAddExerciseDialog = true }) {
                                    Text("管理训练动作", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            } else if (displayDay.exercises.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxHeight(0.4f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("暂无动作安排", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                            isEditable = isEditingAllowed,
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
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("管理训练动作", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (exerciseToDelete != null) {
        val exercise = ExerciseProvider.exercises.find { it.id == exerciseToDelete!!.id }
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("确认删除动作", fontWeight = FontWeight.Bold) },
            text = { Text("确定要将“${exercise?.let { stringResource(it.nameRes) } ?: exerciseToDelete!!.id}”从当天的计划中移除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val updatedExercises = displayDay.exercises.filter { it.id != exerciseToDelete!!.id }
                    viewModel.updatePlanDay(selectedDayOfWeek, updatedExercises.isEmpty(), updatedExercises)
                    exerciseToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
            isInitiallyRest = displayDay.isRest,
            onDismiss = { showAddExerciseDialog = false },
            onSave = { updatedExercises, isRest ->
                viewModel.updatePlanDay(selectedDayOfWeek, isRest, updatedExercises)
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

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onDaySelect(dayNum) }
            ) {
                Text(
                    text = days[dayNum - 1],
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isActualToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else if (isRest) MaterialTheme.colorScheme.outline else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    if (isFullyCompleted && !isRest) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp), tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    } else if (isRest) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                    }
                }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFinished) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isFinished) BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable(enabled = isTrainingAllowed) { onStart() }
            ) {
                Text(
                    stringResource(exercise.targetMuscleRes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(exercise.nameRes), 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (completedCount.toFloat() / planned.targetSets).coerceIn(0f, 1f) },
                        modifier = Modifier.width(60.dp).height(4.dp).clip(CircleShape),
                        color = if (isFinished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "$completedCount / ${planned.targetSets} 组",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isFinished) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
            } else if (isTrainingAllowed) {
                FilledIconButton(
                    onClick = onStart,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                }
            }

            if (isEditable) {
                IconButton(onClick = onDelete, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun QuickAddExerciseDialog(
    currentExercises: List<PlannedExercise>,
    isInitiallyRest: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<PlannedExercise>, Boolean) -> Unit
) {
    var isRest by remember { mutableStateOf(isInitiallyRest) }
    val selectedMap = remember { 
        mutableStateMapOf<String, Int>().apply {
            currentExercises.forEach { put(it.id, it.targetSets) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理训练动作", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("设为休息日", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Switch(checked = isRest, onCheckedChange = { isRest = it })
                }
                
                if (!isRest) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                        items(ExerciseProvider.exercises) { exercise ->
                            val currentSets = selectedMap[exercise.id]
                            val isChecked = currentSets != null
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    if (isChecked) selectedMap.remove(exercise.id) else selectedMap[exercise.id] = 3
                                }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isChecked, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(exercise.nameRes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(stringResource(exercise.targetMuscleRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                
                                if (isChecked) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { 
                                                val s = selectedMap[exercise.id] ?: 3
                                                if (s > 1) selectedMap[exercise.id] = s - 1
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp)) }
                                        Text(
                                            text = "${selectedMap[exercise.id]}", 
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { 
                                                val s = selectedMap[exercise.id] ?: 3
                                                selectedMap[exercise.id] = s + 1
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val updatedList = selectedMap.map { PlannedExercise(it.key, it.value) }
                    onSave(updatedList, isRest) 
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("保存修改", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
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
