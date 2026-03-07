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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.data.ExerciseProvider
import com.fitness.model.PlannedExercise
import com.fitness.model.RoutineDay
import com.fitness.util.getString
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlansScreen(
    currentRoutine: List<RoutineDay>,
    setsByDate: Map<String, List<com.fitness.model.ExerciseSet>>,
    onStartExercise: (String, String) -> Unit,
    onDayClick: (String) -> Unit,
    onUpdatePlanDay: (Int, Boolean, List<PlannedExercise>) -> Unit
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
                        getString("nav_plans"), 
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
                    currentRoutine = currentRoutine,
                    setsByDate = setsByDate,
                    onStartExercise = onStartExercise,
                    onDayClick = onDayClick,
                    onUpdatePlanDay = onUpdatePlanDay
                )
            }
        }
    }
}

@Composable
fun WeekSelectorHeader(weekOffset: Int) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val rangeText = "Week $weekOffset" 

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
                        text = getString("current_tag"),
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
    currentRoutine: List<RoutineDay>,
    setsByDate: Map<String, List<com.fitness.model.ExerciseSet>>,
    onStartExercise: (String, String) -> Unit,
    onDayClick: (String) -> Unit,
    onUpdatePlanDay: (Int, Boolean, List<PlannedExercise>) -> Unit
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedDayOfWeek by remember { mutableIntStateOf(1) }
    
    LaunchedEffect(today, weekOffset) {
        if (weekOffset == 0) {
            selectedDayOfWeek = today.dayOfWeek.ordinal + 1
        }
    }

    val selectedDateStr = "2026-03-07" // Placeholder or use kotlinx-datetime properly

    val displayDay = currentRoutine.find { it.dayOfWeek == selectedDayOfWeek } ?: RoutineDay(selectedDayOfWeek, true, emptyList())
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<PlannedExercise?>(null) }

    val recordedSetsForSelectedDay = setsByDate[selectedDateStr] ?: emptyList()

    val isEditingAllowed = (weekOffset == 0)
    val isTrainingAllowed = true

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        WeeklyProgressBarNavigation(
            weekOffset = weekOffset,
            selectedDayOfWeek = selectedDayOfWeek,
            currentRoutine = currentRoutine,
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
                if (weekOffset == 0 && selectedDayOfWeek == today.dayOfWeek.ordinal + 1) {
                    Text(
                        text = getString("current_tag"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TextButton(onClick = { onDayClick(selectedDateStr) }) {
                Text(getString("heatmap_title"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                            Text(getString("rest_badge"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (isEditingAllowed) {
                                TextButton(onClick = { showAddExerciseDialog = true }) {
                                    Text(getString("create_routine"), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            } else if (displayDay.exercises.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxHeight(0.4f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(getString("no_plan"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                        Text(getString("create_routine"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (exerciseToDelete != null) {
        val exercise = ExerciseProvider.exercises.find { it.id == exerciseToDelete!!.id }
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text(getString("edit_day"), fontWeight = FontWeight.Bold) },
            text = { Text("Remove exercise?") },
            confirmButton = {
                TextButton(onClick = {
                    val updatedExercises = displayDay.exercises.filter { it.id != exerciseToDelete!!.id }
                    onUpdatePlanDay(selectedDayOfWeek, updatedExercises.isEmpty(), updatedExercises)
                    exerciseToDelete = null
                }) {
                    Text(getString("logout"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) { Text(getString("dialog_cancel")) }
            }
        )
    }
}

@Composable
fun WeeklyProgressBarNavigation(
    weekOffset: Int,
    selectedDayOfWeek: Int,
    currentRoutine: List<RoutineDay>,
    onDaySelect: (Int) -> Unit
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val todayDayOfWeek = today.dayOfWeek.ordinal + 1

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (1..7).forEach { dayNum ->
            val dayPlan = currentRoutine.find { it.dayOfWeek == dayNum }
            val isRest = dayPlan?.isRest ?: (dayNum == 3 || dayNum == 7)
            
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
                    if (isRest) {
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
                    getString(exercise.targetMuscleKey),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    getString(exercise.nameKey), 
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
                        "$completedCount / ${planned.targetSets}",
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
fun getDayName(day: Int): String {
    return when(day) {
        1 -> getString("day_mon"); 2 -> getString("day_tue"); 3 -> getString("day_wed"); 4 -> getString("day_thu"); 5 -> getString("day_fri"); 6 -> getString("day_sat"); 7 -> getString("day_sun")
        else -> getString("day_unknown")
    }
}
