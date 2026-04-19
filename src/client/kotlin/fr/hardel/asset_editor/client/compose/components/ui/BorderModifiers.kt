package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Draws an L-shaped border that traces the full top and left edges of the box and
 * sweeps a 90° arc along the top-left corner. Right and bottom edges are not drawn.
 *
 * Matches the TSX reference pattern `border-t-2 border-l-2 border-zinc-900 rounded-xl`,
 * which the naive `drawLine(top) + drawLine(left)` approach cannot reproduce — the
 * straight lines don't follow the rounded corner and cut a visible notch at the top-left.
 *
 * The stroke sits flush against the outer edge (centered at half-stroke inset), so pair
 * this with a matching rounded background/clip on the same [cornerRadius].
 */
fun Modifier.topLeftBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp
): Modifier = drawWithCache {
    val sw = width.toPx()
    val r = cornerRadius.toPx()
    val half = sw / 2f
    val arcRect = Rect(half, half, 2f * r - half, 2f * r - half)

    val path = Path().apply {
        moveTo(half, size.height)
        lineTo(half, r)
        arcTo(arcRect, 180f, 90f, false)
        lineTo(size.width, half)
    }
    val stroke = Stroke(width = sw, cap = StrokeCap.Butt)
    onDrawBehind {
        drawPath(path, color, style = stroke)
    }
}
