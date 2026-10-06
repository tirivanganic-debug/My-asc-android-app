package com.ascendant.sentiment.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ascendant.sentiment.model.SentimentBar
import com.ascendant.sentiment.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

// v3.2: bright magenta highlight for aspects involving these planets
private val MagentaBright = Color(0xFFFF00FF)
private val MAGENTA_PLANETS = setOf("Pluto", "Mercury", "Saturn")

private fun mcLineColor(bar: SentimentBar, highlightMagenta: Boolean): Color {
    val m = bar.mcScore
    return if (highlightMagenta && bar.mcAspects.any { it.planetName in MAGENTA_PLANETS }) MagentaBright
    else if (m > 0.15) Emerald400
    else if (m < -0.15) Rose400
    else Slate400
}

@Composable
fun SentimentChart(
    series: List<SentimentBar>,
    inspectedTimeMs: Long,
    onInspectTime: (Long) -> Unit,
    showMidheaven: Boolean = true,
    highlightMagenta: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth().height(260.dp)
) {
    if (series.isEmpty()) return

    Canvas(
        modifier = modifier
            .pointerInput(series) {
                detectTapGestures { offset ->
                    val plotWidth = size.width - 48.dp.toPx()
                    val relX = offset.x - 42.dp.toPx()
                    if (relX >= 0 && relX <= plotWidth) {
                        val barWidth = plotWidth / series.size
                        val idx = (relX / barWidth).toInt().coerceIn(0, series.size - 1)
                        onInspectTime(series[idx].timeMs)
                    }
                }
            }
            .pointerInput(series) {
                detectDragGestures { change, _ ->
                    val plotWidth = size.width - 48.dp.toPx()
                    val relX = change.position.x - 42.dp.toPx()
                    if (relX >= 0 && relX <= plotWidth) {
                        val barWidth = plotWidth / series.size
                        val idx = (relX / barWidth).toInt().coerceIn(0, series.size - 1)
                        onInspectTime(series[idx].timeMs)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val leftMargin = 42.dp.toPx()
        val bottomMargin = 24.dp.toPx()
        val topMargin = 12.dp.toPx()
        val plotWidth = width - leftMargin - 12.dp.toPx()
        val plotHeight = height - bottomMargin - topMargin

        // Find max Y for scaling
        var maxScore = 0.5
        series.forEach {
            val a = abs(it.sentimentScore)
            if (a > maxScore) maxScore = a
            if (showMidheaven) {
                val m = abs(it.mcScore)
                if (m > maxScore) maxScore = m
            }
        }
        val maxVal = max(0.5, (maxScore * 10).toInt() / 10.0 + 0.15)
        fun getY(v: Double): Float = (topMargin + (plotHeight / 2.0) * (1.0 - v / maxVal)).toFloat()

        // Background (v2.0: semi-transparent so the Active Aspects behind it show through)
        drawRect(color = BgDark.copy(alpha = 0.55f), size = size)

        // Bullish Band
        val yBull = getY(0.15)
        drawRect(
            color = Emerald500.copy(alpha = 0.12f),
            topLeft = Offset(leftMargin, topMargin),
            size = Size(plotWidth, max(0f, yBull - topMargin))
        )

        // Bearish Band
        val yBear = getY(-0.15)
        drawRect(
            color = Rose500.copy(alpha = 0.12f),
            topLeft = Offset(leftMargin, yBear),
            size = Size(plotWidth, topMargin + plotHeight - yBear)
        )

        // Threshold grid lines
        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        val yZero = getY(0.0)

        // Zero line
        drawLine(
            color = Color(0xFF475569),
            start = Offset(leftMargin, yZero),
            end = Offset(leftMargin + plotWidth, yZero),
            strokeWidth = 1.5f
        )

        // +0.15 line
        drawLine(
            color = Emerald500.copy(alpha = 0.4f),
            start = Offset(leftMargin, yBull),
            end = Offset(leftMargin + plotWidth, yBull),
            strokeWidth = 1f,
            pathEffect = dashedEffect
        )

        // -0.15 line
        drawLine(
            color = Rose500.copy(alpha = 0.4f),
            start = Offset(leftMargin, yBear),
            end = Offset(leftMargin + plotWidth, yBear),
            strokeWidth = 1f,
            pathEffect = dashedEffect
        )

        // Draw 5-min bars
        val numBars = series.size
        val barWidth = plotWidth / numBars

        series.forEachIndexed { i, bar ->
            val isBull = bar.sentimentScore > 0.15
            val isBear = bar.sentimentScore < -0.15
            val magentaBar = highlightMagenta && bar.aspects.any { it.planetName in MAGENTA_PLANETS }
            val barColor = if (magentaBar) MagentaBright else if (isBull) Emerald500 else if (isBear) Rose500 else Color(0xFF64748B)

            val yVal = getY(bar.sentimentScore)
            val bx = leftMargin + i * barWidth
            val bTop = minOf(yZero, yVal)
            val bHeight = maxOf(abs(yVal - yZero), if (bar.sentimentScore != 0.0) 1f else 0f)

            drawRect(
                color = barColor.copy(alpha = if (magentaBar) 0.9f else 0.8f),
                topLeft = Offset(bx, bTop),
                size = Size(maxOf(barWidth - 1f, 1.5f), bHeight)
            )
        }

        // v3.2: Midheaven vs birth chart, thin fully opaque lines over the bars (dark halo keeps them visible)
        if (showMidheaven) {
            val lineW = max(1.5f, minOf(barWidth * 0.5f, 3.dp.toPx()))
            series.forEachIndexed { i, bar ->
                val m = bar.mcScore
                if (m != 0.0) {
                    val x = leftMargin + (i + 0.5f) * barWidth
                    val y = getY(m)
                    drawLine(
                        color = Color.Black.copy(alpha = 0.85f),
                        start = Offset(x, yZero),
                        end = Offset(x, y),
                        strokeWidth = lineW + 2f
                    )
                    drawLine(
                        color = mcLineColor(bar, highlightMagenta),
                        start = Offset(x, yZero),
                        end = Offset(x, y),
                        strokeWidth = lineW
                    )
                }
            }
        }

        // Current real-time "Now" vertical dashed line
        val now = System.currentTimeMillis()
        val firstT = series.first().timeMs
        val lastT = series.last().timeMs
        if (now in firstT..lastT) {
            val nowIdx = (now - firstT) / 300_000.0
            val nowX = (leftMargin + nowIdx * barWidth).toFloat()
            drawLine(
                color = Amber500,
                start = Offset(nowX, topMargin),
                end = Offset(nowX, topMargin + plotHeight),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )
        }

        // Inspected Bar vertical cursor
        val selIdx = ((inspectedTimeMs - firstT) / 300_000.0).toInt().coerceIn(0, numBars - 1)
        val selX = leftMargin + (selIdx + 0.5f) * barWidth

        drawLine(
            color = Color.White,
            start = Offset(selX, topMargin),
            end = Offset(selX, topMargin + plotHeight),
            strokeWidth = 2f
        )

        // Target node circle
        val selBar = series[selIdx]
        val selY = getY(selBar.sentimentScore)
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(selX, selY)
        )
        drawCircle(
            color = if (selBar.sentimentScore > 0.15) Emerald400 else if (selBar.sentimentScore < -0.15) Rose400 else Color.Gray,
            radius = 2.5.dp.toPx(),
            center = Offset(selX, selY)
        )

        // Midheaven node on the selected bar
        if (showMidheaven && selBar.mcScore != 0.0) {
            val mcY = getY(selBar.mcScore)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(selX, mcY))
            drawCircle(color = mcLineColor(selBar, highlightMagenta), radius = 1.8.dp.toPx(), center = Offset(selX, mcY))
        }
    }
}
