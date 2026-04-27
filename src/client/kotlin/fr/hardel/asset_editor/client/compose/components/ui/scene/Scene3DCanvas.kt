package fr.hardel.asset_editor.client.compose.components.ui.scene

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import kotlin.math.max
import kotlin.math.sqrt

private const val ZOOM_IN = 1.12f
private const val ZOOM_OUT = 0.88f
private const val ROTATE_X = 0.32f
private const val ROTATE_Y = 0.24f
private const val ZOOM_REFERENCE = 8f
private const val KEY_PAN_STEP = 36f
private const val KEY_MOVE_STEP = 4f
private const val KEY_DELTA_SECONDS = 0.04f
private const val TAP_SLOP = 4f

private fun rotateScale(zoom: Float): Float = sqrt(ZOOM_REFERENCE / max(ZOOM_REFERENCE, zoom))

/** Linear inverse-zoom factor: keeps the visual step roughly constant in pixels regardless of zoom. */
private fun moveScale(zoom: Float): Float = ZOOM_REFERENCE / max(ZOOM_REFERENCE, zoom)

@Composable
fun Scene3DCanvas(
    state: Scene3DState,
    inputKey: Any,
    modifier: Modifier = Modifier,
    overlay: (@Composable () -> Unit)? = null,
    foreground: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    onClick: ((Offset) -> Unit)? = null
) {
    val shape = RoundedCornerShape(10.dp)
    val focusRequester = remember(inputKey) { FocusRequester() }

    LaunchedEffect(inputKey) { runCatching { focusRequester.requestFocus() } }

    LaunchedEffect(state) {
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val deltaSeconds = ((now - previous) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previous = now
            state.tickInertia(deltaSeconds)
        }
    }

    Box(
        modifier = modifier
            .background(StudioColors.Zinc925, shape)
            .border(1.dp, StudioColors.Zinc800, shape)
            .clip(shape)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event -> handleKey(event, state) }
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(inputKey) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            runCatching { focusRequester.requestFocus() }
                        }
                    }
                }
            }
            .pointerInput(inputKey, onClick) { handlePointers(state, focusRequester, onClick) }
            .onSizeChanged { state.viewport = it }
            .clipToBounds()
    ) {
        overlay?.invoke()
        val frame = state.frame
        if (frame != null) {
            Image(
                bitmap = frame.image,
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            placeholder?.let { Box(modifier = Modifier.fillMaxSize().align(Alignment.Center)) { it() } }
        }
        foreground?.invoke()
    }
}

private fun handleKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    state: Scene3DState
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val moveStep = KEY_MOVE_STEP * moveScale(state.camera.zoom)
    when (event.key) {
        Key.Z -> state.applyKeyboardMove(forward = +moveStep, right = 0f, up = 0f, deltaSeconds = KEY_DELTA_SECONDS)
        Key.S -> state.applyKeyboardMove(forward = -moveStep, right = 0f, up = 0f, deltaSeconds = KEY_DELTA_SECONDS)
        Key.Q -> state.applyKeyboardMove(forward = 0f, right = -moveStep, up = 0f, deltaSeconds = KEY_DELTA_SECONDS)
        Key.D -> state.applyKeyboardMove(forward = 0f, right = +moveStep, up = 0f, deltaSeconds = KEY_DELTA_SECONDS)
        Key.Spacebar -> state.applyKeyboardMove(forward = 0f, right = 0f, up = +moveStep, deltaSeconds = KEY_DELTA_SECONDS)
        Key.ShiftLeft, Key.ShiftRight -> state.applyKeyboardMove(forward = 0f, right = 0f, up = -moveStep, deltaSeconds = KEY_DELTA_SECONDS)
        Key.DirectionUp -> state.applyPan(0f, -KEY_PAN_STEP, KEY_DELTA_SECONDS)
        Key.DirectionDown -> state.applyPan(0f, KEY_PAN_STEP, KEY_DELTA_SECONDS)
        Key.DirectionLeft -> state.applyPan(-KEY_PAN_STEP, 0f, KEY_DELTA_SECONDS)
        Key.DirectionRight -> state.applyPan(KEY_PAN_STEP, 0f, KEY_DELTA_SECONDS)
        else -> return false
    }
    return true
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.handlePointers(
    state: Scene3DState,
    focusRequester: FocusRequester,
    onClick: ((Offset) -> Unit)?
) {
    awaitPointerEventScope {
        var lastPosition: Offset? = null
        var lastTimeMillis = 0L
        var pressStart: Offset? = null
        var dragged = false
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Scroll) {
                val change = event.changes.firstOrNull() ?: continue
                val factor = if (change.scrollDelta.y > 0f) ZOOM_OUT else ZOOM_IN
                state.applyZoom(factor, change.position)
                change.consume()
                continue
            }

            val change = event.changes.firstOrNull() ?: continue
            if (!change.pressed) {
                if (!dragged && pressStart != null) onClick?.invoke(change.position)
                lastPosition = null
                pressStart = null
                dragged = false
                state.endDrag()
                continue
            }
            if (lastPosition == null) {
                pressStart = change.position
                dragged = false
                state.beginDrag()
                runCatching { focusRequester.requestFocus() }
            }

            val previous = lastPosition
            val current = change.position
            if (previous != null) {
                val start = pressStart
                if (start != null && !dragged && (current - start).getDistance() > TAP_SLOP) dragged = true
                val delta = current - previous
                val deltaSeconds = ((change.uptimeMillis - lastTimeMillis) / 1000f).coerceAtLeast(0.001f)
                val scale = rotateScale(state.camera.zoom)
                state.applyRotation(delta.x * ROTATE_X * scale, delta.y * ROTATE_Y * scale, deltaSeconds)
                change.consume()
            }
            lastPosition = current
            lastTimeMillis = change.uptimeMillis
        }
    }
}
