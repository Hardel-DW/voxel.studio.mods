package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.hardel.asset_editor.client.compose.VoxelColors

private const val CELL_SIZE = 64f
private val LINE_COLOR = VoxelColors.Zinc950
private val GRID_DIM = Color.Black.copy(alpha = 0.6f)

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

    var x = 0f
    while (x <= w) {
        val snappedX = snap(x)
        drawLine(LINE_COLOR, Offset(snappedX, 0f), Offset(snappedX, h), strokeWidth = 1f)
        x += CELL_SIZE
    }

    var y = 0f
    while (y <= h) {
        val snappedY = snap(y)
        drawLine(LINE_COLOR, Offset(0f, snappedY), Offset(w, snappedY), strokeWidth = 1f)
        y += CELL_SIZE
    }

    val cx = w / 2f
    val cy = h / 2f
    val rx = 0.6f * w
    val ry = 0.5f * h

    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = 1f, scaleY = ry / rx, pivot = Offset.Zero)
    }) {
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.4f to Color.Transparent,
                    1.0f to GRID_DIM
                ),
                center = Offset.Zero,
                radius = 1f
            ),
            topLeft = Offset(-w / (2f * rx), -h / (2f * ry)),
            size = Size(w / rx, h / ry)
        )
    }
}

private fun snap(value: Float): Float = kotlin.math.floor(value) + 0.5f
