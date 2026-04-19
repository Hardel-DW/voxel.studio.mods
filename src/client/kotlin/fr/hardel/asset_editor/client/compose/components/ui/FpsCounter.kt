package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

private const val SAMPLE_WINDOW_NS = 1_000_000_000L

/** FPS + worst-frame overlay; samples `withFrameNanos` and recomposes once per second. */
@Composable
fun FpsCounter(modifier: Modifier = Modifier) {
    val fps = remember { mutableIntStateOf(0) }
    val worstFrameMs = remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var windowStart = 0L
        var framesInWindow = 0
        var previousFrame = 0L
        var worstFrameNs = 0L

        while (true) {
            withFrameNanos { nanos ->
                if (windowStart == 0L) {
                    windowStart = nanos
                    previousFrame = nanos
                    return@withFrameNanos
                }

                val delta = nanos - previousFrame
                if (delta > worstFrameNs) worstFrameNs = delta
                previousFrame = nanos
                framesInWindow++

                val elapsed = nanos - windowStart
                if (elapsed >= SAMPLE_WINDOW_NS) {
                    fps.intValue = (framesInWindow * SAMPLE_WINDOW_NS / elapsed).toInt()
                    worstFrameMs.intValue = (worstFrameNs / 1_000_000L).toInt()
                    windowStart = nanos
                    framesInWindow = 0
                    worstFrameNs = 0L
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${fps.intValue} FPS",
            style = StudioTypography.medium(12),
            color = fpsColor(fps.intValue)
        )
        Text(
            text = "worst ${worstFrameMs.intValue}ms",
            style = StudioTypography.regular(10),
            color = StudioColors.Zinc400
        )
    }
}

private fun fpsColor(fps: Int): Color = when {
    fps >= 55 -> StudioColors.Zinc100
    fps >= 30 -> Color(0xFFFFC857)
    else -> Color(0xFFEF4444)
}
