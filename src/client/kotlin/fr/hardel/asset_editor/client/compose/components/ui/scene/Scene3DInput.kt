package fr.hardel.asset_editor.client.compose.components.ui.scene

import kotlin.math.max
import kotlin.math.sqrt

object Scene3DInput {
    const val KEY_MOVE_PIXELS_PER_TICK: Float = 8f
    const val KEY_PAN_PIXELS_PER_TICK: Float = 24f
    const val KEY_DELTA_SECONDS: Float = 0.04f

    const val KEY_ZOOM_IN_FACTOR: Float = 1.06f
    const val KEY_ZOOM_OUT_FACTOR: Float = 1f / KEY_ZOOM_IN_FACTOR

    const val ROTATE_DEG_PER_PX_X: Float = 0.32f
    const val ROTATE_DEG_PER_PX_Y: Float = 0.24f

    const val WHEEL_ZOOM_IN_FACTOR: Float = 1.12f
    const val WHEEL_ZOOM_OUT_FACTOR: Float = 0.88f

    const val TAP_SLOP: Float = 4f

    private const val SENSITIVITY_REFERENCE_ZOOM: Float = 8f

    fun worldStepForZoom(zoom: Float): Float {
        val safeZoom = if (zoom > 0f) zoom else 1f
        return KEY_MOVE_PIXELS_PER_TICK / safeZoom
    }

    // Rotation/pan should feel finer the closer we get: above the reference zoom
    // we shrink the per-pixel sensitivity so a single drag covers fewer degrees.
    fun rotateSensitivityForZoom(zoom: Float): Float =
        sqrt(SENSITIVITY_REFERENCE_ZOOM / max(SENSITIVITY_REFERENCE_ZOOM, zoom))
}
