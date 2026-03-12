package com.fitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AnatomyMap(
    volumeData: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val neutralColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    
    // Normalize volumes to 0.0 - 1.0 range
    val maxVolume = volumeData.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Define centers for front silhouette
        val centerX = width / 2f
        val topY = height * 0.1f
        
        // Head
        drawCircle(
            color = neutralColor,
            radius = 15.dp.toPx(),
            center = Offset(centerX, topY),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Torso / Chest
        drawMuscleGroup(
            name = "Chest",
            volume = volumeData["cat_chest"] ?: 0.0,
            maxVolume = maxVolume,
            color = primaryColor,
            neutral = neutralColor
        ) { color ->
            drawRoundRect(
                color = color,
                topLeft = Offset(centerX - 40.dp.toPx(), topY + 30.dp.toPx()),
                size = Size(80.dp.toPx(), 40.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
        }
        
        // Shoulders (L/R)
        val shoulderVolume = volumeData["cat_shoulders"] ?: 0.0
        drawMuscleGroup("Shoulders", shoulderVolume, maxVolume, primaryColor, neutralColor) { color ->
            drawCircle(color, 12.dp.toPx(), Offset(centerX - 50.dp.toPx(), topY + 35.dp.toPx()))
            drawCircle(color, 12.dp.toPx(), Offset(centerX + 50.dp.toPx(), topY + 35.dp.toPx()))
        }
        
        // Arms (L/R)
        val armsVolume = volumeData["cat_arms"] ?: 0.0
        drawMuscleGroup("Arms", armsVolume, maxVolume, primaryColor, neutralColor) { color ->
            drawRoundRect(color, Offset(centerX - 65.dp.toPx(), topY + 50.dp.toPx()), Size(15.dp.toPx(), 45.dp.toPx()), CornerRadius(5.dp.toPx()))
            drawRoundRect(color, Offset(centerX + 50.dp.toPx(), topY + 50.dp.toPx()), Size(15.dp.toPx(), 45.dp.toPx()), CornerRadius(5.dp.toPx()))
        }
        
        // Abs / Core
        drawMuscleGroup("Core", volumeData["cat_core"] ?: 0.0, maxVolume, primaryColor, neutralColor) { color ->
            drawRoundRect(color, Offset(centerX - 25.dp.toPx(), topY + 75.dp.toPx()), Size(50.dp.toPx(), 50.dp.toPx()), CornerRadius(10.dp.toPx()))
        }
        
        // Legs (L/R)
        val legsVolume = volumeData["cat_legs"] ?: 0.0
        drawMuscleGroup("Legs", legsVolume, maxVolume, primaryColor, neutralColor) { color ->
            drawRoundRect(color, Offset(centerX - 35.dp.toPx(), topY + 130.dp.toPx()), Size(30.dp.toPx(), 70.dp.toPx()), CornerRadius(8.dp.toPx()))
            drawRoundRect(color, Offset(centerX + 5.dp.toPx(), topY + 130.dp.toPx()), Size(30.dp.toPx(), 70.dp.toPx()), CornerRadius(8.dp.toPx()))
        }
        
        // Outline the whole body for structure
        drawBodyOutline(centerX, topY)
    }
}

private fun DrawScope.drawMuscleGroup(
    name: String,
    volume: Double,
    maxVolume: Double,
    color: Color,
    neutral: Color,
    draw: DrawScope.(Color) -> Unit
) {
    val intensity = (volume / maxVolume).toFloat().coerceIn(0f, 1f)
    val muscleColor = if (volume > 0) {
        color.copy(alpha = 0.3f + (0.7f * intensity))
    } else {
        neutral.copy(alpha = 0.1f)
    }
    draw(muscleColor)
}

private fun DrawScope.drawBodyOutline(centerX: Float, topY: Float) {
    // Simple structural lines
    drawLine(Color.Gray.copy(alpha = 0.3f), Offset(centerX - 40.dp.toPx(), topY + 25.dp.toPx()), Offset(centerX + 40.dp.toPx(), topY + 25.dp.toPx()), strokeWidth = 1.dp.toPx())
}
