package fr.hardel.asset_editor.client.compose.components.ui.scene

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.exp

private const val INERTIA_DAMPING = 7.5f
private const val MIN_VELOCITY = 1f
private const val MIN_ZOOM = 0.02f
private const val MAX_ZOOM = 2048f
private const val MAX_PITCH = 89f

data class Scene3DFrame(val image: ImageBitmap, val camera: Scene3DCamera)

@Stable
class Scene3DState(initialCamera: Scene3DCamera) {
    var camera by mutableStateOf(initialCamera)
        private set
    var viewport by mutableStateOf(IntSize.Zero)
    var frame by mutableStateOf<Scene3DFrame?>(null)

    private var lastFrameKey: String = ""
    private var dragging = false
    private var velYaw = 0f
    private var velPitch = 0f
    private var velPanX = 0f
    private var velPanY = 0f

    fun beginDrag() {
        dragging = true
        velYaw = 0f
        velPitch = 0f
        velPanX = 0f
        velPanY = 0f
    }

    fun endDrag() {
        dragging = false
    }

    fun applyRotation(deltaYaw: Float, deltaPitch: Float, deltaSeconds: Float) {
        camera = camera.copy(
            yaw = camera.yaw + deltaYaw,
            pitch = (camera.pitch + deltaPitch).coerceIn(-MAX_PITCH, MAX_PITCH)
        )
        velYaw = deltaYaw / deltaSeconds
        velPitch = deltaPitch / deltaSeconds
    }

    fun applyPan(deltaX: Float, deltaY: Float, deltaSeconds: Float) {
        camera = camera.copy(panX = camera.panX + deltaX, panY = camera.panY + deltaY)
        velPanX = deltaX / deltaSeconds
        velPanY = deltaY / deltaSeconds
    }

    fun applyZoom(factor: Float, anchor: Offset) {
        val cur = camera
        val next = (cur.zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (next == cur.zoom) return
        val applied = next / cur.zoom
        val localX = anchor.x - viewport.width * 0.5f
        val localY = anchor.y - viewport.height * 0.5f
        camera = cur.copy(
            zoom = next,
            panX = localX - applied * (localX - cur.panX),
            panY = localY - applied * (localY - cur.panY)
        )
    }

    fun tickInertia(deltaSeconds: Float) {
        if (dragging) return
        val hasMotion = abs(velYaw) > MIN_VELOCITY ||
            abs(velPitch) > MIN_VELOCITY ||
            abs(velPanX) > MIN_VELOCITY ||
            abs(velPanY) > MIN_VELOCITY
        if (!hasMotion) return

        camera = camera.copy(
            yaw = camera.yaw + velYaw * deltaSeconds,
            pitch = (camera.pitch + velPitch * deltaSeconds).coerceIn(-MAX_PITCH, MAX_PITCH),
            panX = camera.panX + velPanX * deltaSeconds,
            panY = camera.panY + velPanY * deltaSeconds
        )
        val damp = exp(-INERTIA_DAMPING * deltaSeconds)
        velYaw *= damp
        velPitch *= damp
        velPanX *= damp
        velPanY *= damp
    }

    fun snapTo(target: Scene3DCamera) {
        camera = target
        velYaw = 0f
        velPitch = 0f
        velPanX = 0f
        velPanY = 0f
    }

    fun publishFrame(key: String, image: ImageBitmap, sourceCamera: Scene3DCamera) {
        if (key == lastFrameKey) return
        lastFrameKey = key
        frame = Scene3DFrame(image, sourceCamera)
    }
}
