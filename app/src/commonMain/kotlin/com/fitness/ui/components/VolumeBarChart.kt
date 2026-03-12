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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun VolumeBarChart(
    data: Map<String, Double>,
    labels: Map<String, String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    val valueStyle = TextStyle(
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (data.isEmpty()) return@Canvas

        val maxValue = data.values.maxOrNull() ?: 1.0
        val normalizedMax = if (maxValue < 100.0) 100.0 else maxValue

        val barHeight = 24.dp.toPx()
        val spacing = 16.dp.toPx()
        
        // 留出左侧文字空间 (比如 "cat_chest")
        val textWidthReserve = 100.dp.toPx()
        // 留出右侧数值空间 (比如 "5000 kg")
        val valueWidthReserve = 60.dp.toPx()
        
        val maxBarWidth = size.width - textWidthReserve - valueWidthReserve
        
        var currentY = 0f

        data.entries.sortedByDescending { it.value }.forEach { (category, value) ->
            val ratio = (value / normalizedMax).toFloat().coerceIn(0f, 1f)
            val barWidth = maxBarWidth * ratio

            // Draw Category Label
            val labelText = labels[category] ?: category
            val labelLayout = textMeasurer.measure(labelText, style = labelStyle)
            drawText(
                textLayoutResult = labelLayout,
                topLeft = Offset(0f, currentY + (barHeight - labelLayout.size.height) / 2f)
            )

            // Draw Track (Background Bar)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(textWidthReserve, currentY),
                size = Size(maxBarWidth, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f)
            )

            // Draw Value Bar
            if (barWidth > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(textWidthReserve, currentY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2f)
                )
            }

            // Draw Value Text
            val valueText = "${value.roundToInt()} kg"
            val valueLayout = textMeasurer.measure(valueText, style = valueStyle)
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(
                    textWidthReserve + maxBarWidth + 8.dp.toPx(), 
                    currentY + (barHeight - valueLayout.size.height) / 2f
                )
            )

            currentY += barHeight + spacing
        }
    }
}
