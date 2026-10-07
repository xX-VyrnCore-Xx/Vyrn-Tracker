package com.vyrn.tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Slice(val label: String, val value: Float, val color: Color)

data class BarSeries(val color: Color, val values: List<Float>)

@Composable
fun DonutChart(slices: List<Slice>, modifier: Modifier = Modifier, center: @Composable () -> Unit = {}) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.17f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
            } else {
                var start = -90f
                slices.forEach { s ->
                    val sweep = s.value / total * 360f
                    drawArc(s.color, start, (sweep - 1.5f).coerceAtLeast(0.5f), false, topLeft, arcSize, style = Stroke(strokeWidth))
                    start += sweep
                }
            }
        }
        center()
    }
}

@Composable
fun BarChart(
    series: List<BarSeries>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    maxValue: Float? = null,
) {
    val maxV = (maxValue ?: series.flatMap { it.values }.maxOrNull() ?: 0f).coerceAtLeast(0.001f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = labels.size
            if (n == 0 || series.isEmpty()) return@Canvas
            val groupW = size.width / n
            val barW = groupW * 0.6f / series.size
            for (i in 0 until n) {
                series.forEachIndexed { si, s ->
                    val v = s.values.getOrElse(i) { 0f }.coerceAtLeast(0f)
                    val h = (v / maxV * size.height).coerceAtLeast(3f)
                    val x = i * groupW + groupW * 0.2f + si * barW
                    drawRoundRect(
                        color = if (v <= 0f) track else s.color,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW * 0.9f, h),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
