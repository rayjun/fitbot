package com.fitness.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import com.fitness.data.ExerciseProvider
import com.fitness.data.WorkoutRepository
import com.fitness.model.ExerciseSet
import com.fitness.ui.components.CompactTopAppBar
import com.fitness.util.DateUtils
import com.fitness.util.getString
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRecordingScreen(
    exerciseId: String,
    date: String,
    repository: WorkoutRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val exercise = remember(exerciseId) { ExerciseProvider.exercises.find { it.id == exerciseId } }
    val localizedName = exercise?.let { getString(it.nameKey) } ?: exerciseId
    val isBodyweight = exercise?.isBodyweight ?: false
    val isCardio = exercise?.categoryKey == "cat_cardio"
    val isToday = remember(date) { date == DateUtils.getTodayString() }

    val setsFlow = remember(repository, date) { repository.getSetsByDate(date) }
    val sets by setsFlow.collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<ExerciseSet?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CompactTopAppBar(
                title = localizedName,
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (isToday) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Set")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(getString("session_history"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val filteredSets = sets.filter { it.exerciseName == exerciseId }
                if (filteredSets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(getString("history_empty"), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    items(filteredSets) { set ->
                        SetItemRow(
                            set = set, 
                            isBodyweight = isBodyweight,
                            isCardio = isCardio,
                            onClick = if (isToday) { { editingSet = set } } else null
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecordSetDialog(
            isBodyweight = isBodyweight,
            isCardio = isCardio,
            onDismiss = { showAddDialog = false },
            onSave = { weight, reps, distance, duration ->
                val now = Clock.System.now()
                val localTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                val timeStr = "${localTime.hour.toString().padStart(2, '0')}:${localTime.minute.toString().padStart(2, '0')}"
                val newSet = ExerciseSet(
                    date = date,
                    sessionId = "Session_${now.toEpochMilliseconds()}",
                    exerciseName = exerciseId,
                    reps = reps,
                    weight = weight,
                    distance = distance,
                    duration = duration,
                    timestamp = now.toEpochMilliseconds(),
                    timeStr = timeStr
                )
                showAddDialog = false
                scope.launch {
                    repository.addExerciseSet(newSet)
                }
            }
        )
    }

    if (editingSet != null) {
        val set = editingSet!!
        RecordSetDialog(
            initialWeight = set.weight.toString(),
            initialReps = set.reps.toString(),
            initialDistance = set.distance?.toString() ?: "",
            initialDuration = set.duration?.toString() ?: "",
            isBodyweight = isBodyweight,
            isCardio = isCardio,
            isEdit = true,
            onDismiss = { editingSet = null },
            onSave = { weight, reps, distance, duration ->
                editingSet?.let { currentSet ->
                    scope.launch {
                        repository.updateExerciseSet(currentSet.copy(weight = weight, reps = reps, distance = distance, duration = duration))
                    }
                }
                editingSet = null
            },
            onDelete = {
                editingSet?.let { currentSet ->
                    scope.launch {
                        repository.deleteExerciseSet(currentSet.id, currentSet.date)
                    }
                }
                editingSet = null
            }
        )
    }
}

@Composable
private fun SetItemRow(set: ExerciseSet, isBodyweight: Boolean, isCardio: Boolean, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth().let {
            if (onClick != null) it.clickable { onClick() } else it
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        ListItem(
            headlineContent = { 
                val text = if (isCardio) {
                    val dist = set.distance ?: 0.0
                    val dur = set.duration ?: 0
                    "${dur} ${getString("minutes")} | ${dist} ${getString("km")}"
                } else if (isBodyweight) {
                    "${set.reps} ${getString("reps")}"
                } else {
                    "${set.weight} ${getString("kg")} x ${set.reps}"
                }
                Text(text, fontWeight = FontWeight.Bold) 
            },
            trailingContent = { Text(set.timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun RecordSetDialog(
    initialWeight: String = "0",
    initialReps: String = "12",
    initialDistance: String = "",
    initialDuration: String = "",
    isBodyweight: Boolean,
    isCardio: Boolean,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Double, Int, Double?, Int?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var weightInput by remember { mutableStateOf(initialWeight) }
    var repsInput by remember { mutableStateOf(initialReps) }
    var distanceInput by remember { mutableStateOf(initialDistance) }
    var durationInput by remember { mutableStateOf(initialDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) getString("edit_record") else getString("add_record")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isCardio) {
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text(getString("duration_min")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = { distanceInput = it },
                        label = { Text(getString("distance_km")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (!isBodyweight) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text(getString("weight_kg")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { repsInput = it },
                        label = { Text(getString("reps")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (isCardio) {
                    val dist = distanceInput.toDoubleOrNull()
                    val dur = durationInput.toIntOrNull()
                    onSave(0.0, 0, dist, dur)
                } else {
                    val w = if (isBodyweight) 0.0 else (weightInput.toDoubleOrNull() ?: 0.0)
                    val r = repsInput.toIntOrNull() ?: 0
                    onSave(w, r, null, null)
                }
            }) {
                Text(getString("save"))
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getString("delete"))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(getString("dialog_cancel"))
                }
            }
        }
    )
}
