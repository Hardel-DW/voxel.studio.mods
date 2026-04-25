package fr.hardel.asset_editor.client.compose.components.ui.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import fr.hardel.asset_editor.client.compose.StudioColors

data class GridBounds(val sizeX: Int, val sizeY: Int, val sizeZ: Int)

@Composable
fun IsometricGrid(
    state: Scene3DState,
    bounds: GridBounds,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val viewport = state.viewport
        if (viewport.width <= 0 || viewport.height <= 0) return@Canvas

        val camera = state.frame?.camera ?: state.camera
        val sx = bounds.sizeX
        val sz = bounds.sizeZ
        val cy = -bounds.sizeY * 0.5f
        val centerX = sx * 0.5f
        val centerZ = sz * 0.5f
        val trig = Scene3DProjection.trig(camera)

        val minor = StudioColors.Zinc800.copy(alpha = 0.45f)
        val major = StudioColors.Zinc700.copy(alpha = 0.5f)

        for (x in 0..sx) {
            val color = if (x == 0 || x == sx) major else minor
            val width = if (x == 0 || x == sx) 1.5f else 1f
            drawLine(
                color = color,
                start = Scene3DProjection.project(camera, viewport, trig, x - centerX, cy, -centerZ),
                end = Scene3DProjection.project(camera, viewport, trig, x - centerX, cy, sz - centerZ),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
        for (z in 0..sz) {
            val color = if (z == 0 || z == sz) major else minor
            val width = if (z == 0 || z == sz) 1.5f else 1f
            drawLine(
                color = color,
                start = Scene3DProjection.project(camera, viewport, trig, -centerX, cy, z - centerZ),
                end = Scene3DProjection.project(camera, viewport, trig, sx - centerX, cy, z - centerZ),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
}
