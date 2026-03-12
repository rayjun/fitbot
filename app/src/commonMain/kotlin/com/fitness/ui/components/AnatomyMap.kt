package com.fitness.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Canvas
import fitnesstracker.app.generated.resources.Res
import fitnesstracker.app.generated.resources.anatomy_back_mosaic
import fitnesstracker.app.generated.resources.anatomy_front_mosaic
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Pixel-art / Mosaic style anatomy map.
 * Uses a pixelated base image and blocky rectangle heatmaps to match the 8-bit aesthetic.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun AnatomyMap(
    volumeData: Map<String, Double>,
    isBackView: Boolean,
    onMuscleClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val maxVolume = volumeData.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(isBackView, boxSize) {
                detectTapGestures { offset ->
                    if (boxSize != IntSize.Zero) {
                        val size = Size(boxSize.width.toFloat(), boxSize.height.toFloat())
                        val tappedMuscle = detectImageHit(offset, size, isBackView)
                        onMuscleClick(tappedMuscle)
                    }
                }
            }
    ) {
        val imageRes = if (isBackView) Res.drawable.anatomy_back_mosaic else Res.drawable.anatomy_front_mosaic
        
        // 1. Heatmap Canvas (Underneath the pixel lines)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val imageAspect = 1f / 2.2f
            val boxAspect = size.width / size.height
            
            val drawWidth: Float
            val drawHeight: Float
            val startX: Float
            val startY: Float
            
            if (boxAspect > imageAspect) {
                drawHeight = size.height
                drawWidth = drawHeight * imageAspect
                startX = (size.width - drawWidth) / 2f
                startY = 0f
            } else {
                drawWidth = size.width
                drawHeight = drawWidth / imageAspect
                startX = 0f
                startY = (size.height - drawHeight) / 2f
            }

            clipRect(startX, startY, startX + drawWidth, startY + drawHeight) {
                if (!isBackView) {
                    drawFrontOverlays(startX, startY, drawWidth, drawHeight, volumeData, maxVolume, primaryColor)
                } else {
                    drawBackOverlays(startX, startY, drawWidth, drawHeight, volumeData, maxVolume, primaryColor)
                }
            }
        }

        // 2. Mosaic Blueprint Lines (Tinted pixel art)
        Image(
            painter = painterResource(imageRes),
            contentDescription = "Anatomy Map",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(lineColor),
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun DrawScope.drawFrontOverlays(
    startX: Float, startY: Float, width: Float, height: Float,
    volumeData: Map<String, Double>, maxVolume: Double, color: Color
) {
    val cx = startX + width / 2f
    val cy = startY

    fun drawMuscle(region: () -> Unit, volumeKey: String) {
        val vol = volumeData[volumeKey] ?: 0.0
        val intensity = (vol / maxVolume).toFloat().coerceIn(0f, 1f)
        val fillAlpha = if (vol > 0) 0.5f + (0.5f * intensity) else 0.05f
        
        drawContext.canvas.save()
        region.invoke()
        drawContext.canvas.restore()
    }

    val activeColor = color

    // Chest (Pecs) - Blocky Rects
    drawMuscle({
        val vol = volumeData["cat_chest"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.24f, cy + height * 0.23f), size = Size(width * 0.23f, height * 0.08f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.01f, cy + height * 0.23f), size = Size(width * 0.23f, height * 0.08f))
    }, "cat_chest")

    // Core (Abs)
    drawMuscle({
        val vol = volumeData["cat_core"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.16f, cy + height * 0.32f), size = Size(width * 0.32f, height * 0.18f))
    }, "cat_core")

    // Shoulders (Delts)
    drawMuscle({
        val vol = volumeData["cat_shoulders"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.38f, cy + height * 0.20f), size = Size(width * 0.16f, height * 0.09f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.22f, cy + height * 0.20f), size = Size(width * 0.16f, height * 0.09f))
    }, "cat_shoulders")

    // Arms (Biceps/Forearms)
    drawMuscle({
        val vol = volumeData["cat_arms"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.44f, cy + height * 0.29f), size = Size(width * 0.14f, height * 0.22f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.30f, cy + height * 0.29f), size = Size(width * 0.14f, height * 0.22f))
    }, "cat_arms")

    // Legs (Quads)
    drawMuscle({
        val vol = volumeData["cat_legs"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.26f, cy + height * 0.51f), size = Size(width * 0.22f, height * 0.40f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.04f, cy + height * 0.51f), size = Size(width * 0.22f, height * 0.40f))
    }, "cat_legs")
}

private fun DrawScope.drawBackOverlays(
    startX: Float, startY: Float, width: Float, height: Float,
    volumeData: Map<String, Double>, maxVolume: Double, color: Color
) {
    val cx = startX + width / 2f
    val cy = startY

    fun drawMuscle(region: () -> Unit, volumeKey: String) {
        val vol = volumeData[volumeKey] ?: 0.0
        val intensity = (vol / maxVolume).toFloat().coerceIn(0f, 1f)
        val fillAlpha = if (vol > 0) 0.5f + (0.5f * intensity) else 0.05f
        
        drawContext.canvas.save()
        region()
        drawContext.canvas.restore()
    }

    val activeColor = color

    // Back (Lats/Traps)
    drawMuscle({
        val vol = volumeData["cat_back"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.26f, cy + height * 0.20f), size = Size(width * 0.52f, height * 0.28f))
    }, "cat_back")

    // Shoulders (Rear Delts)
    drawMuscle({
        val vol = volumeData["cat_shoulders"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.40f, cy + height * 0.20f), size = Size(width * 0.16f, height * 0.09f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.24f, cy + height * 0.20f), size = Size(width * 0.16f, height * 0.09f))
    }, "cat_shoulders")

    // Arms (Triceps)
    drawMuscle({
        val vol = volumeData["cat_arms"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.46f, cy + height * 0.29f), size = Size(width * 0.14f, height * 0.20f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.32f, cy + height * 0.29f), size = Size(width * 0.14f, height * 0.20f))
    }, "cat_arms")

    // Legs (Glutes/Hamstrings)
    drawMuscle({
        val vol = volumeData["cat_legs"] ?: 0.0
        val alpha = if (vol > 0) 0.5f + (0.5f * (vol/maxVolume).toFloat()) else 0.05f
        // Glutes
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.28f, cy + height * 0.48f), size = Size(width * 0.26f, height * 0.12f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.02f, cy + height * 0.48f), size = Size(width * 0.26f, height * 0.12f))
        // Hamstrings/Calves
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx - width * 0.26f, cy + height * 0.60f), size = Size(width * 0.22f, height * 0.35f))
        drawRect(activeColor.copy(alpha = alpha), topLeft = Offset(cx + width * 0.04f, cy + height * 0.60f), size = Size(width * 0.22f, height * 0.35f))
    }, "cat_legs")
}

private fun detectImageHit(offset: Offset, size: Size, isBackView: Boolean): String? {
    val imageAspect = 1f / 2.2f
    val boxAspect = size.width / size.height
    
    val width: Float
    val height: Float
    val startX: Float
    val startY: Float
    
    if (boxAspect > imageAspect) {
        height = size.height
        width = height * imageAspect
        startX = (size.width - width) / 2f
        startY = 0f
    } else {
        width = size.width
        height = width / imageAspect
        startX = 0f
        startY = (size.height - height) / 2f
    }

    val x = offset.x - startX
    val y = offset.y - startY
    
    if (x < 0 || x > width || y < 0 || y > height) return null
    
    val relY = y / height
    val relX = x / width

    if (!isBackView) {
        if (relY in 0.23..0.32 && relX in 0.22..0.78) return "cat_chest"
        if (relY in 0.34..0.52 && relX in 0.32..0.68) return "cat_core"
        if (relY in 0.55..0.85 && relX in 0.22..0.78) return "cat_legs"
        if (relY in 0.29..0.50 && (relX < 0.32 || relX > 0.68)) return "cat_arms"
        if (relY in 0.21..0.28 && (relX < 0.35 || relX > 0.65)) return "cat_shoulders"
    } else {
        if (relY in 0.21..0.47 && relX in 0.22..0.78) return "cat_back"
        if (relY in 0.49..0.87 && relX in 0.2..0.8) return "cat_legs"
        if (relY in 0.28..0.50 && (relX < 0.28 || relX > 0.72)) return "cat_arms"
        if (relY in 0.21..0.28 && (relX < 0.35 || relX > 0.65)) return "cat_shoulders"
    }
    return null
}
