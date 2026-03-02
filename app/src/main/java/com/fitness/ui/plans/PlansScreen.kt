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
    onStartPlan: (Int) -> Unit
) {
    val currentRoutine by viewModel.currentRoutine.collectAsStateWithLifecycle()
    // 0 = 本周, -1 = 上周, 1 = 下周
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
                onStartPlan = onStartPlan
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
        "本周 (${mondayOfSelectedWeek.format(formatter)} - ${sundayOfSelectedWeek.format(formatter)})"
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
    onStartPlan: (Int) -> Unit
) {
    var editingDayOfWeek by remember { mutableIntStateOf(-1) }
    val setsToday by workoutViewModel.setsToday.collectAsStateWithLifecycle()
    
    // 我们需要一个状态来追踪点击圆圈选中的那一天（1-7）
    var selectedDayOfWeek by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    
    // 如果切换了周，默认选中周一（或者如果是本周，默认选中今天）
    LaunchedEffect(weekOffset) {
        selectedDayOfWeek = if (weekOffset == 0) LocalDate.now().dayOfWeek.value else 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (routine.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(R.string.no_plan), color = MaterialTheme.outlineVariant())
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
            // Weekly Progress Bar (Now clickable to switch day view)
            WeeklyProgressBar(
                routine = routine, 
                workoutViewModel = workoutViewModel, 
                weekOffset = weekOffset,
                selectedDayOfWeek = selectedDayOfWeek,
                onDaySelect = { selectedDayOfWeek = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Task Card for Selected Day
            val selectedRoutine = routine.find { it.dayOfWeek == selectedDayOfWeek }
            
            // 获取选中那一天的真实日期字符串
            val selectedDateStr = remember(selectedDayOfWeek, weekOffset) {
                val today = LocalDate.now()
                val monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val targetDate = monday.plusWeeks(weekOffset.toLong()).plusDays((selectedDayOfWeek - 1).toLong())
                targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            
            // 异步获取选中那一天的锻炼记录
            var recordedSetsForDay by remember { mutableStateOf<List<com.fitness.data.local.SetEntity>>(emptyList()) }
            LaunchedEffect(selectedDateStr, setsToday) {
                recordedSetsForDay = workoutViewModel.getSetsByDate(selectedDateStr)
            }

            TaskDetailCard(
                routineDay = selectedRoutine, 
                dateStr = selectedDateStr,
                recordedSets = recordedSetsForDay,
                isToday = (weekOffset == 0 && selectedDayOfWeek == LocalDate.now().dayOfWeek.value),
                onStartPlan = { onStartPlan(selectedDayOfWeek) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.week_overview), fontWeight = FontWeight.Bold)
                if (weekOffset == 0) {
                    TextButton(onClick = { /* Could add full edit mode here */ }) {
                        Text("点击列表修改计划", fontSize = 12.sp)
                    }
                }
            }
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
                            if (weekOffset == 0 && day.dayOfWeek == LocalDate.now().dayOfWeek.value) {
                                SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.today_badge)) })
                            }
                        },
                        modifier = Modifier.clickable { 
                            if (weekOffset == 0) editingDayOfWeek = day.dayOfWeek 
                            selectedDayOfWeek = day.dayOfWeek
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
fun TaskDetailCard(
    routineDay: RoutineDay?, 
    dateStr: String,
    recordedSets: List<com.fitness.data.local.SetEntity>,
    isToday: Boolean,
    onStartPlan: () -> Unit
) {
    val isCompleted = remember(recordedSets, routineDay) {
        if (routineDay == null || routineDay.isRest) false
        else routineDay.exercises.isNotEmpty() && routineDay.exercises.all { planned ->
            val count = recordedSets.count { it.exerciseName == planned.id }
            count >= planned.targetSets
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isToday) stringResource(R.string.today_task) else dateStr, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (isCompleted) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (routineDay == null || routineDay.isRest) {
                Text(stringResource(R.string.rest_message), style = MaterialTheme.typography.bodyLarge)
            } else {
                if (isCompleted) {
                    Text(stringResource(R.string.training_completed), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    Text(stringResource(R.string.exercises_scheduled, routineDay.exercises.size))
                }
                
                // 展示该日完成的动作摘要
                if (recordedSets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val grouped = recordedSets.groupBy { it.exerciseName }
                    grouped.forEach { (id, sets) ->
                        val exercise = ExerciseProvider.exercises.find { it.id == id }
                        Text(
                            text = "• ${exercise?.let { stringResource(it.nameRes) } ?: id}: ${sets.size} 组",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (isToday) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onStartPlan, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCompleted) "继续训练 (Continue)" else stringResource(R.string.start_training))
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyProgressBar(
    routine: List<RoutineDay>, 
    workoutViewModel: WorkoutViewModel, 
    weekOffset: Int,
    selectedDayOfWeek: Int,
    onDaySelect: (Int) -> Unit
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
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            // 计算这个圆圈对应的真实日期
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

            val isSelected = selectedDayOfWeek == day.dayOfWeek
            val isActualToday = weekOffset == 0 && day.dayOfWeek == todayDayOfWeek

            val color = when {
                day.isRest -> Color.Transparent
                isFullyCompleted -> MaterialTheme.colorScheme.primary
                isActualToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val borderColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                day.isRest -> MaterialTheme.colorScheme.outline
                else -> Color.Transparent
            }
            val borderWidth = if (isSelected) 3.dp else 1.dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(borderWidth, borderColor, CircleShape)
                    .clickable { onDaySelect(day.dayOfWeek) }
            ) {
                Text(
                    text = days[day.dayOfWeek - 1],
                    color = if (isFullyCompleted && !day.isRest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = if (isSelected) 16.sp else 14.sp
                )
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

// 辅助方法：获取 Material3 的 outlineVariant 颜色，处理兼容性
@Composable
fun MaterialTheme.outlineVariant(): Color = colorScheme.onSurface.copy(alpha = 0.3f)
