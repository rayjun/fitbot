package com.fitness.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.data.ExerciseProvider
import com.fitness.ui.components.ExerciseImage
import com.fitness.util.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit
) {
    val exercise = remember(exerciseId) { ExerciseProvider.exercises.find { it.id == exerciseId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.let { getString(it.nameKey) } ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (exercise != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExerciseImage(
                        gifResPath = exercise.gifResPath,
                        contentDescription = getString(exercise.nameKey),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = getString("detail_target_muscle"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = getString(exercise.targetMuscleKey), modifier = Modifier.padding(top = 4.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = getString("detail_instructions"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = getString(exercise.descriptionKey), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
