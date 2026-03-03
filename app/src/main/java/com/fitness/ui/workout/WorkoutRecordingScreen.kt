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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import com.fitness.R
import com.fitness.data.ExerciseProvider
import com.fitness.data.local.SetEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRecordingScreen(
    exerciseId: String,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val exercise = remember(exerciseId) { ExerciseProvider.exercises.find { it.id == exerciseId } }
    val localizedName = exercise?.let { stringResource(it.nameRes) } ?: exerciseId
    val isBodyweight = exercise?.isBodyweight ?: false

    val sets by viewModel.setsToday.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<SetEntity?>(null) }

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
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Set")
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
            Text(stringResource(R.string.session_history), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // 已保存组数列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val filteredSets = sets.filter { it.exerciseName == exerciseId }
                if (filteredSets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("今天还没有记录，点击右下角开始吧！", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    items(filteredSets) { set ->
                        SetItemRow(
                            set = set, 
                            isBodyweight = isBodyweight,
                            onClick = { editingSet = set }
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
                viewModel.addSet(exerciseId, weight, reps)
                showAddDialog = false
            }
        )
    }

    if (editingSet != null) {
        RecordSetDialog(
            initialWeight = editingSet!!.weight.toString(),
            initialReps = editingSet!!.reps.toString(),
            isBodyweight = isBodyweight,
            isEdit = true,
            onDismiss = { editingSet = null },
            onSave = { weight, reps ->
                viewModel.updateSet(editingSet!!.copy(weight = weight, reps = reps))
                editingSet = null
            },
            onDelete = {
                viewModel.deleteSet(editingSet!!.id, editingSet!!.date)
                editingSet = null
            }
        )
    }
}

@Composable
fun SetItemRow(set: SetEntity, isBodyweight: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        ListItem(
            headlineContent = { 
                val text = if (isBodyweight) {
                    "${set.reps} ${stringResource(R.string.reps)}"
                } else {
                    "${set.weight} kg x ${set.reps}"
                }
                Text(text, fontWeight = FontWeight.Bold) 
            },
            trailingContent = { Text(set.timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun RecordSetDialog(
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
        title = { Text(if (isEdit) "修改记录" else "添加组数") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isBodyweight) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text(stringResource(R.string.weight_kg)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text(stringResource(R.string.reps)) },
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
    )
}
