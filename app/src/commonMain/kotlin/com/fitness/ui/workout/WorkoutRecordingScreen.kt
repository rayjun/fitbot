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
    val isToday = remember(date) { date == DateUtils.getTodayString() }

    val setsFlow = remember(repository, date) { repository.getSetsByDate(date) }
    val sets by setsFlow.collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<ExerciseSet?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedName, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            onDismiss = { showAddDialog = false },
            onSave = { weight, reps ->
                val now = Clock.System.now()
                val localTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                val timeStr = "${localTime.hour.toString().padStart(2, '0')}:${localTime.minute.toString().padStart(2, '0')}"
                val newSet = ExerciseSet(
                    date = date,
                    sessionId = "Session_${now.toEpochMilliseconds()}",
                    exerciseName = exerciseId,
                    reps = reps,
                    weight = weight,
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
            isBodyweight = isBodyweight,
            isEdit = true,
            onDismiss = { editingSet = null },
            onSave = { weight, reps ->
                editingSet?.let { currentSet ->
                    scope.launch {
                        repository.updateExerciseSet(currentSet.copy(weight = weight, reps = reps))
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
private fun SetItemRow(set: ExerciseSet, isBodyweight: Boolean, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth().let {
            if (onClick != null) it.clickable { onClick() } else it
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        ListItem(
            headlineContent = { 
                val text = if (isBodyweight) {
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
    isBodyweight: Boolean,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var weightInput by remember { mutableStateOf(initialWeight) }
    var repsInput by remember { mutableStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) getString("edit_record") else getString("add_record")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        },
        confirmButton = {
            Button(onClick = { 
                val w = if (isBodyweight) 0.0 else (weightInput.toDoubleOrNull() ?: 0.0)
                val r = repsInput.toIntOrNull() ?: 0
                onSave(w, r)
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
