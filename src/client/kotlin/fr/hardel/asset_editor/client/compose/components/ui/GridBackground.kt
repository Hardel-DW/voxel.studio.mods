package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

@Composable
fun GridBackground(
    modifier: Modifier = Modifier,
    cellSize: Dp = 36.dp,
    accent: Color = StudioColors.Zinc500
) {
    val timeNanos by produceState(0L) {
        while (true) withFrameNanos { value = it }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawGrid(cellSize.toPx(), accent, timeNanos / 1_000_000_000f)
    }
}

private val LineColor = Color(0xFF161618)
private val VignetteColor = Color.Black.copy(alpha = 0.55f)
private const val PulseDurationS = 1.6f
private const val MinPeriodS = 4.5f
private const val MaxPeriodS = 11f
private const val MinPeakAlpha = 0.06f
private const val MaxPeakAlpha = 0.18f
private const val InsetPx = 1f

private fun DrawScope.drawGrid(cell: Float, accent: Color, time: Float) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f || cell <= 0f) return

    drawGridLines(w, h, cell)
    drawTwinkles(w, h, cell, accent, time)
    drawVignette(w, h)
}

private fun DrawScope.drawGridLines(w: Float, h: Float, cell: Float) {
    var x = 0f
    while (x <= w) {
        val sx = floor(x) + 0.5f
        drawLine(LineColor, Offset(sx, 0f), Offset(sx, h), strokeWidth = 1f)
        x += cell
    }
    var y = 0f
    while (y <= h) {
        val sy = floor(y) + 0.5f
        drawLine(LineColor, Offset(0f, sy), Offset(w, sy), strokeWidth = 1f)
        y += cell
    }
}

private fun DrawScope.drawTwinkles(w: Float, h: Float, cell: Float, accent: Color, time: Float) {
    val cols = ceil(w / cell).toInt()
    val rows = ceil(h / cell).toInt()
    val cellRect = Size(cell - 2f * InsetPx, cell - 2f * InsetPx)
    for (i in 0 until cols) {
        for (j in 0 until rows) {
            val alpha = pulseAlpha(i, j, time)
            if (alpha <= 0f) continue
            drawRect(
                color = accent.copy(alpha = alpha),
                topLeft = Offset(i * cell + InsetPx, j * cell + InsetPx),
                size = cellRect
            )
        }
    }
}

private fun DrawScope.drawVignette(w: Float, h: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.55f to Color.Transparent,
                1f to VignetteColor
            ),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = max(w, h) * 0.7f
        ),
        topLeft = Offset.Zero,
        size = Size(w, h)
    )
}

private fun pulseAlpha(i: Int, j: Int, time: Float): Float {
    val hash = ((i * 92821) xor (j * 37579) xor ((i + j) * 14107)).toLong() and 0xFFFFFFFFL
    val periodNorm = ((hash shr 8) and 0xFFFF) / 65535f
    val period = MinPeriodS + (MaxPeriodS - MinPeriodS) * periodNorm
    val phase = (hash and 0xFF) / 255f
    val elapsed = (((time / period) + phase) % 1f) * period
    if (elapsed > PulseDurationS) return 0f
    val curve = sin(PI.toFloat() * elapsed / PulseDurationS)
    val peakNorm = ((hash shr 24) and 0xFF) / 255f
    val peak = MinPeakAlpha + (MaxPeakAlpha - MinPeakAlpha) * peakNorm
    return curve * peak
}
