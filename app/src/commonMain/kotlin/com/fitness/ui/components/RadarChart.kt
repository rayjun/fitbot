package com.fitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fitness.data.ExerciseProvider
import com.fitness.util.getString
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun RadarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    // 固定的 6 大核心肌群
    val categories = listOf("cat_chest", "cat_back", "cat_legs", "cat_arms", "cat_shoulders", "cat_core")
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f * 0.7f // 留出空间写字
        val center = Offset(size.width / 2f, size.height / 2f)
        val anglePerCategory = 2 * PI / categories.size

        // 1. Draw Web (Grid)
        val gridLevels = 4
        for (level in 1..gridLevels) {
            val levelRadius = radius * (level.toFloat() / gridLevels)
            val path = Path()
            for (i in categories.indices) {
                val angle = i * anglePerCategory - PI / 2 // 从正上方开始 (-90度)
                val x = center.x + levelRadius * cos(angle).toFloat()
                val y = center.y + levelRadius * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = gridColor,
                style = Stroke(width = 1f)
            )
        }

        // Draw axes
        for (i in categories.indices) {
            val angle = i * anglePerCategory - PI / 2
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            drawLine(
                color = gridColor,
                start = center,
                end = Offset(x, y),
                strokeWidth = 1f
            )
        }

        // 2. Draw Data Polygon
        val maxValue = data.values.maxOrNull() ?: 1.0 // 避免除以 0
        val normalizedMax = if (maxValue < 1000.0) 1000.0 else maxValue // 设定一个下限，避免刚开始练的时候图表直接撑满

        val dataPath = Path()
        val dataPoints = mutableListOf<Offset>()

        for (i in categories.indices) {
            val cat = categories[i]
            val value = data[cat] ?: 0.0
            val ratio = (value / normalizedMax).toFloat().coerceIn(0f, 1f)
            
            val angle = i * anglePerCategory - PI / 2
            val pointRadius = radius * ratio
            val x = center.x + pointRadius * cos(angle).toFloat()
            val y = center.y + pointRadius * sin(angle).toFloat()
            
            val point = Offset(x, y)
            dataPoints.add(point)
            
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(
            path = dataPath,
            color = color.copy(alpha = 0.3f),
            style = Fill
        )
        drawPath(
            path = dataPath,
            color = color,
            style = Stroke(width = 3f)
        )

        // Draw points on data corners
        dataPoints.forEach { point ->
            drawCircle(
                color = color,
                radius = 4f,
                center = point
            )
        }

        // 3. Draw Labels
        for (i in categories.indices) {
            val cat = categories[i]
            val angle = i * anglePerCategory - PI / 2
            val labelRadius = radius * 1.25f // 标签在最外圈
            val x = center.x + labelRadius * cos(angle).toFloat()
            val y = center.y + labelRadius * sin(angle).toFloat()

            // 我们需要调用 getString()，但这在 Canvas 里不能直接调用 Composable。
            // 为了简化，我们需要在外部翻译好传进来，或者暂时用 key。
            // 稍后我们会在调用方翻译并传入，这里先暂时直接绘制。
            val textLayoutResult = textMeasurer.measure(
                text = cat, // 这里先放原始 key，稍后通过参数传入 map
                style = labelStyle
            )
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x - textLayoutResult.size.width / 2f,
                    y - textLayoutResult.size.height / 2f
                )
            )
        }
    }
}
