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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors

@Composable
fun Scene3DCanvas(
    state: Scene3DState,
    inputKey: Any,
    modifier: Modifier = Modifier,
    zoomOnCursor: Boolean = true,
    overlay: (@Composable () -> Unit)? = null,
    foreground: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    onClick: ((Offset) -> Unit)? = null
) {
    val shape = RoundedCornerShape(10.dp)
    val focusRequester = remember(inputKey) { FocusRequester() }
    var hasFocus by remember(inputKey) { mutableStateOf(false) }
    val zoomOnCursorRef = rememberUpdatedState(zoomOnCursor)

    val refocus: () -> Unit = remember(focusRequester) {
        { if (!hasFocus) runCatching { focusRequester.requestFocus() } }
    }

    LaunchedEffect(inputKey) { runCatching { focusRequester.requestFocus() } }

    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused, inputKey) {
        if (windowFocused) runCatching { focusRequester.requestFocus() }
    }

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
            .onFocusChanged { hasFocus = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event -> handleKey(event, state) }
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(inputKey) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        refocus()
                    }
                }
            }
            .pointerInput(inputKey, onClick) { handleGestures(state, refocus, onClick, zoomOnCursorRef) }
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

private fun handleKey(event: KeyEvent, state: Scene3DState): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val moveStep = Scene3DInput.worldStepForZoom(state.camera.zoom)
    val panStep = Scene3DInput.KEY_PAN_PIXELS_PER_TICK
    val deltaSeconds = Scene3DInput.KEY_DELTA_SECONDS
    when (event.key) {
        Key.Z -> state.applyZoomCentered(Scene3DInput.KEY_ZOOM_IN_FACTOR)
        Key.S -> state.applyZoomCentered(Scene3DInput.KEY_ZOOM_OUT_FACTOR)
        Key.Q -> state.applyKeyboardMove(forward = 0f, right = -moveStep, up = 0f, deltaSeconds = deltaSeconds)
        Key.D -> state.applyKeyboardMove(forward = 0f, right = +moveStep, up = 0f, deltaSeconds = deltaSeconds)
        Key.Spacebar -> state.applyKeyboardMove(forward = 0f, right = 0f, up = +moveStep, deltaSeconds = deltaSeconds)
        Key.ShiftLeft, Key.ShiftRight -> state.applyKeyboardMove(forward = 0f, right = 0f, up = -moveStep, deltaSeconds = deltaSeconds)
        Key.DirectionUp -> state.applyPan(0f, +panStep, deltaSeconds)
        Key.DirectionDown -> state.applyPan(0f, -panStep, deltaSeconds)
        Key.DirectionLeft -> state.applyPan(+panStep, 0f, deltaSeconds)
        Key.DirectionRight -> state.applyPan(-panStep, 0f, deltaSeconds)
        else -> return false
    }
    return true
}

private suspend fun PointerInputScope.handleGestures(
    state: Scene3DState,
    refocus: () -> Unit,
    onClick: ((Offset) -> Unit)?,
    zoomOnCursor: androidx.compose.runtime.State<Boolean>
) {
    awaitPointerEventScope {
        val drag = DragSession()
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Scroll) {
                handleScroll(state, event.changes.firstOrNull() ?: continue, zoomOnCursor.value)
                continue
            }
            val change = event.changes.firstOrNull() ?: continue
            if (!change.pressed) {
                drag.release(state, change, onClick)
                continue
            }
            drag.advance(state, change, refocus)
        }
    }
}

private fun handleScroll(state: Scene3DState, change: PointerInputChange, zoomOnCursor: Boolean) {
    val factor = if (change.scrollDelta.y > 0f) Scene3DInput.WHEEL_ZOOM_OUT_FACTOR else Scene3DInput.WHEEL_ZOOM_IN_FACTOR
    if (zoomOnCursor) state.applyZoom(factor, change.position) else state.applyZoomCentered(factor)
    change.consume()
}

private class DragSession {
    private var lastPosition: Offset? = null
    private var lastTimeMillis = 0L
    private var pressStart: Offset? = null
    private var dragged = false

    fun advance(state: Scene3DState, change: PointerInputChange, refocus: () -> Unit) {
        if (lastPosition == null) {
            pressStart = change.position
            dragged = false
            state.beginDrag()
            refocus()
            lastPosition = change.position
            lastTimeMillis = change.uptimeMillis
            return
        }
        val previous = lastPosition!!
        val current = change.position
        val start = pressStart
        if (start != null && !dragged && (current - start).getDistance() > Scene3DInput.TAP_SLOP) dragged = true
        val delta = current - previous
        val deltaSeconds = ((change.uptimeMillis - lastTimeMillis) / 1000f).coerceAtLeast(0.001f)
        val sensitivity = Scene3DInput.rotateSensitivityForZoom(state.camera.zoom)
        state.applyRotation(
            deltaYaw = delta.x * Scene3DInput.ROTATE_DEG_PER_PX_X * sensitivity,
            deltaPitch = delta.y * Scene3DInput.ROTATE_DEG_PER_PX_Y * sensitivity,
            deltaSeconds = deltaSeconds
        )
        change.consume()
        lastPosition = current
        lastTimeMillis = change.uptimeMillis
    }

    fun release(state: Scene3DState, change: PointerInputChange, onClick: ((Offset) -> Unit)?) {
        val start = pressStart
        if (start != null && !dragged) onClick?.invoke(change.position)
        lastPosition = null
        pressStart = null
        dragged = false
        state.endDrag()
    }
}
