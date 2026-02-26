package com.fitness.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRecordingScreen(
    exerciseName: String,
    viewModel: WorkoutViewModel,
    onFinished: () -> Unit
) {
    var weightInput by remember { mutableStateOf("0") }
    var repsInput by remember { mutableStateOf("12") }
    val sets by viewModel.setsInSession.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("正在记录: $exerciseName") })
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
                    label = { Text("重量 (kg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("次数") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.addSet(exerciseName, weightInput.toDoubleOrNull() ?: 0.0, repsInput.toIntOrNull() ?: 0)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存此组记录")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("当前动作组数记录:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 已保存组数列表
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sets.filter { it.exerciseName == exerciseName }) { set ->
                    ListItem(
                        headlineContent = { Text("${set.weight} kg x ${set.reps} 次") },
                        trailingContent = { Text(set.timeStr) }
                    )
                }
            }

            // 完成按钮
            Button(
                onClick = {
                    viewModel.finishWorkout()
                    onFinished()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("完成训练并同步")
            }
        }
    }
}
