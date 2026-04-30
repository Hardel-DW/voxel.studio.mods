package fr.hardel.asset_editor.client.compose.components.ui.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin

object Scene3DProjection {
    fun trig(camera: Scene3DCamera): SceneTrig {
        val yawRad = Math.toRadians(camera.yaw.toDouble())
        val pitchRad = Math.toRadians(camera.pitch.toDouble())
        return SceneTrig(
            cosYaw = cos(yawRad).toFloat(),
            sinYaw = sin(yawRad).toFloat(),
            cosPitch = cos(pitchRad).toFloat(),
            sinPitch = sin(pitchRad).toFloat()
        )
    }

    fun project(
        camera: Scene3DCamera,
        viewport: IntSize,
        trig: SceneTrig,
        cx: Float,
        cy: Float,
        cz: Float
    ): Offset {
        val x1 = cx * trig.cosYaw + cz * trig.sinYaw
        val z1 = -cx * trig.sinYaw + cz * trig.cosYaw
        val y2 = cy * trig.cosPitch - z1 * trig.sinPitch
        return Offset(
            viewport.width * 0.5f + camera.panX + x1 * camera.zoom,
            viewport.height * 0.5f + camera.panY - y2 * camera.zoom
        )
    }
}

data class SceneTrig(
    val cosYaw: Float,
    val sinYaw: Float,
    val cosPitch: Float,
    val sinPitch: Float
)
