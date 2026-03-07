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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Compose Multiplatform resources 1.6.11 supports loading files from commonMain/composeResources/files/
        // We use the generated Res class if available, or painterResource with the path.
        // For animated GIFs on iOS, standard painterResource might only show the first frame.
        // But it's better than a text placeholder.
        Image(
            painter = painterResource(gifResPath),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}
