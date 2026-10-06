package com.ascendant.sentiment.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascendant.sentiment.model.SentimentBar
import com.ascendant.sentiment.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Colour of the thin Midheaven line for one bar. */
private fun mcLineColor(bar: SentimentBar): Color {
    val m = bar.mcScore
    return if (m > 0.15) Emerald400 else if (m < -0.15) Rose400 else Slate400
}

/**
 * 5-minute sentiment bars on a fixed -1 to +1 scale (scale labels sit in the left gap).
 * zoom = 1 shows every bar; higher values show fewer bars, wider, around the selected bar.
 * Pinch with two fingers, or use the zoom buttons, to change zoom (onZoomBy gets a multiplier).
 */
@Composable
fun SentimentChart(
    series: List<SentimentBar>,
    inspectedTimeMs: Long,
    onInspectTime: (Long) -> Unit,
    zoom: Float = 1f,
    onZoomBy: (Float) -> Unit = {},
    showMidheaven: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth().height(260.dp)
) {
    if (series.isEmpty()) return

    val numBars = series.size
    val firstT = series.first().timeMs
    val count = (numBars / zoom).roundToInt().coerceIn(minOf(12, numBars), numBars)
    val selIdx = ((inspectedTimeMs - firstT) / 300_000.0).toInt().coerceIn(0, numBars - 1)

    // view[0] = first visible bar, view[1] = visible bar count, view[2] = zoom the view was last centred for
    val view = remember(series) { intArrayOf(0, numBars, 1000) }
    val zoomKey = (zoom * 1000f).roundToInt()
    var start = view[0]
    if (zoomKey != view[2]) start = selIdx - count / 2            // zoom changed: centre on the cursor
    else if (selIdx < start) start = selIdx                       // cursor left the view: scroll just enough
    else if (selIdx >= start + count) start = selIdx - count + 1
    start = start.coerceIn(0, numBars - count)
    view[0] = start
    view[1] = count
    view[2] = zoomKey

    val currentOnInspect = rememberUpdatedState(onInspectTime)
    val currentOnZoom = rememberUpdatedState(onZoomBy)

    Canvas(
        modifier = modifier
            .pointerInput(series) {
                awaitEachGesture {
                    val leftPx = 42.dp.toPx()
                    val plotW = size.width - leftPx - 12.dp.toPx()

                    // clamp = true: touches stay inside the plot. clamp = false (dragging): going past an
                    // edge selects bars beyond it, which scrolls the zoomed view.
                    fun idxAt(x: Float, clamp: Boolean): Int {
                        val barW = plotW / view[1]
                        val rel = if (clamp) {
                            ((x - leftPx).coerceIn(0f, plotW - 0.001f) / barW).toInt()
                        } else {
                            kotlin.math.floor((x - leftPx) / barW).toInt()
                        }
                        return (view[0] + rel).coerceIn(0, numBars - 1)
                    }

                    var lastIdx = -1
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var idx = idxAt(down.position.x, true)
                    lastIdx = idx
                    currentOnInspect.value(series[idx].timeMs)

                    var prevDist = 0f
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            // two fingers: pinch to zoom
                            val dist = (pressed[0].position - pressed[1].position).getDistance()
                            if (prevDist > 0f && dist > 0f) currentOnZoom.value(dist / prevDist)
                            prevDist = dist
                            event.changes.forEach { it.consume() }
                        } else if (pressed.size == 1) {
                            // one finger: move the white cursor
                            prevDist = 0f
                            val c = pressed[0]
                            if (c.positionChanged()) {
                                idx = idxAt(c.position.x, false)
                                if (idx != lastIdx) {
                                    lastIdx = idx
                                    currentOnInspect.value(series[idx].timeMs)
                                }
                                c.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
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

        // Fixed index scale: -1 to +1
        val maxVal = 1.0
        fun getY(v: Double): Float = (topMargin + (plotHeight / 2.0) * (1.0 - v / maxVal)).toFloat()

        val barWidth = plotWidth / count
        val firstVisible = view[0]

        // Background (semi-transparent so the Active Aspects behind it show through)
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

        // Scale grid lines and tick marks at +-0.5 and +-1
        listOf(1.0, 0.5, -0.5, -1.0).forEach { v ->
            val gy = getY(v)
            drawLine(
                color = Color(0xFF475569).copy(alpha = 0.5f),
                start = Offset(leftMargin - 4.dp.toPx(), gy),
                end = Offset(leftMargin + plotWidth, gy),
                strokeWidth = 1f
            )
        }

        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        val yZero = getY(0.0)

        // Zero line
        drawLine(
            color = Color(0xFF475569),
            start = Offset(leftMargin - 4.dp.toPx(), yZero),
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

        // 5-min bars (visible slice only)
        for (i in firstVisible until firstVisible + count) {
            val bar = series[i]
            val isBull = bar.sentimentScore > 0.15
            val isBear = bar.sentimentScore < -0.15
            val barColor = if (isBull) Emerald500 else if (isBear) Rose500 else Color(0xFF64748B)

            val yVal = getY(bar.sentimentScore)
            val bx = leftMargin + (i - firstVisible) * barWidth
            val bTop = minOf(yZero, yVal)
            val bHeight = maxOf(abs(yVal - yZero), if (bar.sentimentScore != 0.0) 1f else 0f)

            drawRect(
                color = barColor.copy(alpha = 0.8f),
                topLeft = Offset(bx, bTop),
                size = Size(maxOf(barWidth - 1f, 1.5f), bHeight)
            )
        }

        // Midheaven vs birth chart: thin fully opaque lines over the bars (dark halo keeps them visible)
        if (showMidheaven) {
            val lineW = max(1.5f, minOf(barWidth * 0.5f, 3.dp.toPx()))
            for (i in firstVisible until firstVisible + count) {
                val bar = series[i]
                val m = bar.mcScore
                if (m != 0.0) {
                    val x = leftMargin + (i - firstVisible + 0.5f) * barWidth
                    val y = getY(m)
                    drawLine(
                        color = Color.Black.copy(alpha = 0.85f),
                        start = Offset(x, yZero),
                        end = Offset(x, y),
                        strokeWidth = lineW + 2f
                    )
                    drawLine(
                        color = mcLineColor(bar),
                        start = Offset(x, yZero),
                        end = Offset(x, y),
                        strokeWidth = lineW
                    )
                }
            }
        }

        // Current real-time "Now" vertical dashed line
        val now = System.currentTimeMillis()
        val nowIdx = (now - firstT) / 300_000.0
        if (nowIdx >= firstVisible && nowIdx <= firstVisible + count) {
            val nowX = (leftMargin + (nowIdx - firstVisible) * barWidth).toFloat()
            drawLine(
                color = Amber500,
                start = Offset(nowX, topMargin),
                end = Offset(nowX, topMargin + plotHeight),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )
        }

        // Inspected bar vertical cursor
        val selX = leftMargin + (selIdx - firstVisible + 0.5f) * barWidth
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
            drawCircle(color = mcLineColor(selBar), radius = 1.8.dp.toPx(), center = Offset(selX, mcY))
        }

        // Vertical index scale in the left gap
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        fun scaleLabel(text: String, v: Double, color: Color) {
            labelPaint.color = color.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                text,
                leftMargin - 7.dp.toPx(),
                getY(v) + labelPaint.textSize * 0.35f,
                labelPaint
            )
        }
        scaleLabel("+1", 1.0, Slate400)
        scaleLabel("+0.5", 0.5, Slate400)
        scaleLabel("+0.15", 0.15, Emerald400.copy(alpha = 0.8f))
        scaleLabel("0", 0.0, Slate400)
        scaleLabel("−0.15", -0.15, Rose400.copy(alpha = 0.8f))
        scaleLabel("−0.5", -0.5, Slate400)
        scaleLabel("−1", -1.0, Slate400)
    }
}
