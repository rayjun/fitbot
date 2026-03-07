package com.fitness.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun ExerciseImage(
    gifResPath: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    // On iOS, we use painterResource to load the image from composeResources.
    // Note: Standard painterResource might not play animated GIFs by default in all CMP versions,
    // but this is the idiomatic way to load the resource.
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource("files/$gifResPath"),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}
