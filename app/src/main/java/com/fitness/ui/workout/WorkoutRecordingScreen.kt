package com.fitness.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.fitness.R
import com.fitness.data.ExerciseProvider

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

    var weightInput by remember { mutableStateOf("0") }
    var repsInput by remember { mutableStateOf("12") }
    val sets by viewModel.setsInSession.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedName, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // 输入区域
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(stringResource(R.string.weight_kg)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text(stringResource(R.string.reps)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.addSet(exerciseId, weightInput.toDoubleOrNull() ?: 0.0, repsInput.toIntOrNull() ?: 0)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_set))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.session_history), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 已保存组数列表
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sets.filter { it.exerciseName == exerciseId }) { set ->
                    ListItem(
                        headlineContent = { Text("${set.weight} kg x ${set.reps}") },
                        trailingContent = { Text(set.timeStr) }
                    )
                }
            }
        }
    }
}
