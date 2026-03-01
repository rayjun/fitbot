package com.fitness.ui.library

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.fitness.R
import com.fitness.data.ExerciseProvider
import com.fitness.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    isCloudConnected: Boolean,
    isSyncing: Boolean,
    onConnectCloud: () -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    val allLabelRes = R.string.category_all
    var selectedCategoryRes by remember { mutableIntStateOf(allLabelRes) }

    val filteredExercises = if (selectedCategoryRes == allLabelRes) {
        ExerciseProvider.exercises
    } else {
        ExerciseProvider.exercises.filter { it.categoryRes == selectedCategoryRes }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_library)) }, 
                actions = {
                    IconButton(onClick = onConnectCloud) {
                        if (isSyncing) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Syncing",
                                tint = Color(0xFFFFA000), // Orange
                                modifier = Modifier.rotate(rotation)
                            )
                        } else {
                            Icon(
                                imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = stringResource(R.string.cloud_connect),
                                tint = if (isCloudConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val categories = ExerciseProvider.categories

            // 顶部部位筛选 Tab
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategoryRes).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                categories.forEach { categoryRes ->
                    Tab(
                        selected = selectedCategoryRes == categoryRes,
                        onClick = { selectedCategoryRes = categoryRes },
                        text = { 
                            Text(
                                text = stringResource(categoryRes),
                                style = MaterialTheme.typography.titleSmall
                            ) 
                        }
                    )
                }
            }

            // 动作网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable { onExerciseClick(exercise) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/${exercise.gifResPath}")
                        .decoderFactory(
                            if (android.os.Build.VERSION.SDK_INT >= 28) {
                                ImageDecoderDecoder.Factory()
                            } else {
                                GifDecoder.Factory()
                            }
                        )
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(exercise.nameRes),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Text(
                text = stringResource(exercise.nameRes),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
