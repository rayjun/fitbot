package com.fitness.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.data.ExerciseProvider
import com.fitness.model.Exercise
import com.fitness.ui.components.ExerciseImage
import com.fitness.util.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onExerciseClick: (Exercise) -> Unit
) {
    val allLabelKey = "category_all"
    var selectedCategoryKey by remember { mutableStateOf(allLabelKey) }

    val filteredExercises = if (selectedCategoryKey == allLabelKey) {
        ExerciseProvider.exercises
    } else {
        ExerciseProvider.exercises.filter { it.categoryKey == selectedCategoryKey }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        getString("nav_library"),
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
        Column(modifier = Modifier.padding(padding)) {
            val categories = ExerciseProvider.categories

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategoryKey).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (categories.isNotEmpty()) {
                        val selectedIndex = categories.indexOf(selectedCategoryKey).coerceAtLeast(0)
                        if (selectedIndex < tabPositions.size) {
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedIndex])
                                    .height(3.dp)
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                    }
                }
            ) {
                categories.forEach { categoryKey ->
                    val isSelected = selectedCategoryKey == categoryKey
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategoryKey = categoryKey },
                        text = { 
                            Text(
                                text = getString(categoryKey),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredExercises) { exercise ->
                    ExerciseGridItem(exercise, onExerciseClick)
                }
            }
        }
    }
}

@Composable
fun ExerciseGridItem(exercise: Exercise, onExerciseClick: (Exercise) -> Unit) {
    val isDark = isSystemInDarkTheme()
    
    Card(
        onClick = { onExerciseClick(exercise) },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Image Container: Creates a "paper stage" for the white-bg GIF
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isDark) Color(0xFFF0F2F5).copy(alpha = 0.9f) 
                        else Color.White
                    ),
                contentAlignment = Alignment.Center
            ) {
                ExerciseImage(
                    gifResPath = exercise.gifResPath,
                    contentDescription = getString(exercise.nameKey),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = getString(exercise.nameKey),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = getString(exercise.targetMuscleKey),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
