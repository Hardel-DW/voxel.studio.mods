package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * A checkmark drawn in two segments that "writes itself in" as [progress] goes from 0 to 1.
 *
 * Shared across any success-confirmation affordance (reload button, copy button, etc.) so every
 * success state uses the exact same shape and timing.
 */
@Composable
fun AnimatedCheckmark(
    progress: Float,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.12f

        val p1 = Offset(w * 0.20f, h * 0.52f)
        val p2 = Offset(w * 0.43f, h * 0.74f)
        val p3 = Offset(w * 0.80f, h * 0.30f)

        val seg1 = (p2 - p1).getDistance()
        val seg2 = (p3 - p2).getDistance()
        val total = seg1 + seg2
        val seg1Frac = if (total == 0f) 0f else seg1 / total

        val path = Path().apply {
            moveTo(p1.x, p1.y)
            if (progress <= seg1Frac) {
                val t = if (seg1Frac == 0f) 0f else progress / seg1Frac
                lineTo(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)
            } else {
                lineTo(p2.x, p2.y)
                val t = (progress - seg1Frac) / (1f - seg1Frac)
                lineTo(p2.x + (p3.x - p2.x) * t, p2.y + (p3.y - p2.y) * t)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
