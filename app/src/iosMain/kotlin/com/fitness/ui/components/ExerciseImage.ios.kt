package com.fitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun ExerciseImage(
    gifResPath: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    // On iOS, direct string-to-painter loading is not supported in the new type-safe resource system.
    // Additionally, animated GIFs require specialized loading on iOS.
    // For now, we use a styled placeholder to unblock the build.
    Box(
        modifier = modifier.background(Color.LightGray.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GIF", 
            color = Color.Gray,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
        )
    }
}
