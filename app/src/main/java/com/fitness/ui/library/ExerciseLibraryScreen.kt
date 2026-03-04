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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
                title = { 
                    Text(
                        stringResource(R.string.nav_library),
                        fontWeight = FontWeight.Bold
                    ) 
                }, 
                actions = {
                    IconButton(onClick = onConnectCloud) {
                        if (isSyncing) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Syncing",
                                tint = MaterialTheme.colorScheme.primary,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val categories = ExerciseProvider.categories

            // 顶部部位筛选 Tab - 移除分割线，增加间距
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategoryRes).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}, // 移除 MD2 风格的分割线
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[categories.indexOf(selectedCategoryRes).coerceAtLeast(0)]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                categories.forEach { categoryRes ->
                    Tab(
                        selected = selectedCategoryRes == categoryRes,
                        onClick = { selectedCategoryRes = categoryRes },
                        text = { 
                            Text(
                                text = stringResource(categoryRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedCategoryRes == categoryRes) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            // 动作网格 - 改为 3 列
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    // 使用 MD3 ElevatedCard
    ElevatedCard(
        onClick = { onExerciseClick(exercise) },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f), // 稍微拉长一点
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
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
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = stringResource(exercise.nameRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(exercise.targetMuscleRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
