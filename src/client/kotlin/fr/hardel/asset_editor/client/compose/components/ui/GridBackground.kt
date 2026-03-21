package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import fr.hardel.asset_editor.client.compose.VoxelColors

private const val CELL_SIZE = 64f

@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawGrid()
    }
}

private fun DrawScope.drawGrid() {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val lineColor = VoxelColors.Zinc950

    var x = 0f
    while (x <= w) {
        drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        x += CELL_SIZE
    }

    var y = 0f
    while (y <= h) {
        drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        y += CELL_SIZE
    }

    val cx = w / 2f
    val cy = h / 2f
    val rx = 0.6f * w
    val ry = 0.5f * h

    val steps = 60
    for (ring in 0..steps) {
        val t = ring.toFloat() / steps
        if (t < 0.4f) continue
        val alpha = ((t - 0.4f) / 0.6f) * 0.6f
        val innerR = t
        val outerR = (ring + 1).toFloat() / steps

        drawOval(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(cx - rx * outerR, cy - ry * outerR),
            size = Size(rx * outerR * 2, ry * outerR * 2)
        )
    }
}
