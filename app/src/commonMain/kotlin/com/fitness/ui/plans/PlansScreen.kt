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
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitness.data.ExerciseProvider
import com.fitness.model.PlannedExercise
import com.fitness.model.RoutineDay
import com.fitness.util.getString
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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

/** Returns the Monday of the week that is [weekOffset] weeks away from today. */
private fun mondayOfWeek(weekOffset: Int): LocalDate {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val daysSinceMonday = (today.dayOfWeek.ordinal) // Mon=0 … Sun=6
    val thisMonday = today.minus(daysSinceMonday, DateTimeUnit.DAY)
    return thisMonday.plus(weekOffset * 7, DateTimeUnit.DAY)
}

@Composable
fun WeekSelectorHeader(weekOffset: Int) {
    val monday = mondayOfWeek(weekOffset)
    val sunday = monday.plus(6, DateTimeUnit.DAY)
    val rangeText = "${monday.monthNumber}/${monday.dayOfMonth} – ${sunday.monthNumber}/${sunday.dayOfMonth}"

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (weekOffset == 0) {
                    Text(
                        text = getString("current_tag"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward, null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val todayDayOfWeek = today.dayOfWeek.ordinal + 1 // 1=Mon … 7=Sun

    // Default selected day: today if viewing current week, else Monday
    var selectedDayOfWeek by remember(weekOffset) {
        mutableIntStateOf(if (weekOffset == 0) todayDayOfWeek else 1)
    }

    // Compute the actual calendar date for the selected day
    val monday = mondayOfWeek(weekOffset)
    val selectedDate: LocalDate = monday.plus(selectedDayOfWeek - 1, DateTimeUnit.DAY)
    val selectedDateStr = selectedDate.toString()

    val displayDay = currentRoutine.find { it.dayOfWeek == selectedDayOfWeek }
        ?: RoutineDay(selectedDayOfWeek, false, emptyList())

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<PlannedExercise?>(null) }
    var exerciseToEditSets by remember { mutableStateOf<PlannedExercise?>(null) }

    val recordedSetsForSelectedDay = setsByDate[selectedDateStr] ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        WeeklyProgressBarNavigation(
            weekOffset = weekOffset,
            selectedDayOfWeek = selectedDayOfWeek,
            currentRoutine = currentRoutine,
            onDaySelect = { selectedDayOfWeek = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Day header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = getDayName(selectedDayOfWeek),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (weekOffset == 0 && selectedDayOfWeek == todayDayOfWeek) {
                    Text(
                        text = getString("current_tag"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Toggle rest / training day
                if (displayDay.isRest) {
                    TextButton(onClick = {
                        onUpdatePlanDay(selectedDayOfWeek, false, displayDay.exercises)
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(getString("set_training_day"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = {
                        onUpdatePlanDay(selectedDayOfWeek, true, displayDay.exercises)
                    }) {
                        Icon(Icons.Default.Hotel, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(getString("set_rest_day"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = { onDayClick(selectedDateStr) }) {
                    Text(getString("heatmap_title"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
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
                    Box(
                        modifier = Modifier.fillParentMaxHeight(0.4f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            getString("rest_badge"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                if (displayDay.exercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.3f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                getString("no_plan"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
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
                                onStart = { onStartExercise(exercise.id, selectedDateStr) },
                                onDelete = { exerciseToDelete = planned },
                                onEditSets = { exerciseToEditSets = planned }
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getString("add_exercise"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            existingExercises = displayDay.exercises,
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { updatedExercises ->
                onUpdatePlanDay(selectedDayOfWeek, false, updatedExercises)
                showAddExerciseDialog = false
            }
        )
    }

    // Adjust sets dialog
    if (exerciseToEditSets != null) {
        val target = exerciseToEditSets!!
        var newSets by remember(target) { mutableIntStateOf(target.targetSets) }
        AlertDialog(
            onDismissRequest = { exerciseToEditSets = null },
            title = {
                val exercise = ExerciseProvider.exercises.find { it.id == target.id }
                Text(exercise?.let { getString(it.nameKey) } ?: target.id, fontWeight = FontWeight.Bold)
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(getString("target_sets"), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { if (newSets > 1) newSets-- }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text("$newSets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (newSets < 10) newSets++ }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = displayDay.exercises.map {
                        if (it.id == target.id) it.copy(targetSets = newSets) else it
                    }
                    onUpdatePlanDay(selectedDayOfWeek, false, updated)
                    exerciseToEditSets = null
                }) {
                    Text(getString("save"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToEditSets = null }) { Text(getString("dialog_cancel")) }
            }
        )
    }

    // Delete confirmation dialog
    if (exerciseToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text(getString("edit_day"), fontWeight = FontWeight.Bold) },
            text = { Text(getString("remove_exercise")) },
            confirmButton = {
                TextButton(onClick = {
                    val updatedExercises = displayDay.exercises.filter { it.id != exerciseToDelete!!.id }
                    onUpdatePlanDay(selectedDayOfWeek, false, updatedExercises)
                    exerciseToDelete = null
                }) {
                    Text(getString("confirm_remove"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
            val isRest = dayPlan?.isRest ?: false

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
                            color = if (isRest && !isSelected) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    if (isRest) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                        )
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
    onStart: () -> Unit,
    onDelete: () -> Unit,
    onEditSets: () -> Unit
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
            // Exercise info + progress — tap to start
            Column(
                modifier = Modifier.weight(1f).clickable { onStart() }
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
                    // Tap the sets count to edit target sets
                    Text(
                        "$completedCount / ${planned.targetSets}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onEditSets() }
                    )
                }
            }

            if (isFinished) {
                Icon(
                    Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                FilledIconButton(
                    onClick = onStart,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.padding(start = 4.dp)) {
                Icon(
                    Icons.Default.DeleteOutline, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun getDayName(day: Int): String {
    return when (day) {
        1 -> getString("day_mon"); 2 -> getString("day_tue"); 3 -> getString("day_wed")
        4 -> getString("day_thu"); 5 -> getString("day_fri"); 6 -> getString("day_sat")
        7 -> getString("day_sun"); else -> getString("day_unknown")
    }
}

@Composable
fun AddExerciseDialog(
    existingExercises: List<PlannedExercise>,
    onDismiss: () -> Unit,
    onConfirm: (List<PlannedExercise>) -> Unit
) {
    val allExercises = remember { ExerciseProvider.exercises }
    // Pre-populate with existing exercises so they appear already checked
    val selectedSets = remember {
        mutableStateMapOf<String, Int>().also { map ->
            existingExercises.forEach { map[it.id] = it.targetSets }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(getString("add_exercise"), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(allExercises) { exercise ->
                    val isSelected = exercise.id in selectedSets
                    val sets = selectedSets[exercise.id] ?: 3
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedSets.remove(exercise.id)
                                else selectedSets[exercise.id] = 3
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedSets[exercise.id] = 3
                                    else selectedSets.remove(exercise.id)
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    getString(exercise.nameKey),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    getString(exercise.targetMuscleKey),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (sets > 1) selectedSets[exercise.id] = sets - 1 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        "$sets",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.widthIn(min = 20.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = { if (sets < 10) selectedSets[exercise.id] = sets + 1 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        getString("target_sets"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedSets.map { (id, sets) -> PlannedExercise(id, sets) })
                },
                enabled = selectedSets.isNotEmpty()
            ) {
                Text(getString("save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(getString("dialog_cancel")) }
        }
    )
}
