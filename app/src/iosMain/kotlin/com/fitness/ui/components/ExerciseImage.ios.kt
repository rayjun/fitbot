package com.fitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    // Placeholder for iOS: Just a Box with text.
    // In a real app, this would use an iOS image loader or CMP Resources.
    Box(
        modifier = modifier.background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "GIF: $gifResPath", color = Color.DarkGray)
    }
}
