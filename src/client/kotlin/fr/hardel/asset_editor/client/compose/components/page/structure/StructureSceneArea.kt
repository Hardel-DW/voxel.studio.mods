package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.scene.GridBounds
import fr.hardel.asset_editor.client.compose.components.ui.scene.IsometricGrid
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCamera
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCanvas
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DState
import kotlin.math.max

private const val ANIM_DURATION_MS = 380f

/**
 * Generic scene container for any [StructureSceneSubject].
 *
 * Drives:
 *  - the off-screen renderer via [StructureSceneSurface]
 *  - the 2D grid overlay
 *  - the camera (default framing, reset signal, inertia)
 *  - the staged drop animation when pagination is enabled
 *
 * @param selectedStage   meaningful only when [StructureSceneSubject.stageCount] > 0.
 *                        For templates pass any value (it is ignored).
 */
@Composable
fun StructureSceneArea(
    subject: StructureSceneSubject,
    selectedStage: Int,
    animations: Boolean,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    modifier: Modifier = Modifier
) {
    val state = remember(subject.id) { Scene3DState(defaultCamera(subject)) }
    val animation = rememberStageAnimation(subject, selectedStage, animations)

    LaunchedEffect(state, subject.id) {
        StructureCameraReset.requests.collect {
            state.snapTo(defaultCamera(subject))
        }
    }

    val filters = StructureSceneFilters(
        displayedStage = animation.displayedStage,
        animatingStage = animation.animatingStage,
        sliceY = sliceY,
        showJigsaws = showJigsaws,
        highlight = highlight,
        drop = animation.dropOffset
    )

    StructureSceneSurface(state = state, subject = subject, filters = filters)

    Scene3DCanvas(
        state = state,
        inputKey = subject.id,
        modifier = modifier,
        overlay = {
            IsometricGrid(
                state = state,
                bounds = GridBounds(subject.sizeX, subject.sizeY, subject.sizeZ),
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

private data class StageAnimation(
    val displayedStage: Int,
    val animatingStage: Int,
    val dropOffset: Float
) {
    companion object {
        val ALL = StageAnimation(displayedStage = Int.MAX_VALUE, animatingStage = -1, dropOffset = 0f)
    }
}

@Composable
private fun rememberStageAnimation(
    subject: StructureSceneSubject,
    selectedStage: Int,
    animations: Boolean
): StageAnimation {
    if (subject.stageCount <= 0) return StageAnimation.ALL

    var lastSettledStage by remember(subject.id) { mutableIntStateOf(selectedStage) }
    var displayedStage by remember(subject.id) { mutableIntStateOf(selectedStage) }
    var animatingStage by remember(subject.id) { mutableIntStateOf(-1) }
    var dropOffset by remember(subject.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(subject.id, selectedStage, animations) {
        val target = selectedStage
        val current = lastSettledStage
        if (target == current) return@LaunchedEffect

        val forward = target > current
        val piece = if (forward) target - 1 else current - 1

        if (!animations || piece < 0) {
            displayedStage = target
            animatingStage = -1
            dropOffset = 0f
            lastSettledStage = target
            return@LaunchedEffect
        }

        displayedStage = max(current, target)
        animatingStage = piece
        val drop = max(subject.sizeY, 12).toFloat()

        try {
            val startNanos = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val elapsed = ((now - startNanos) / 1_000_000f).coerceAtLeast(0f)
                val progress = (elapsed / ANIM_DURATION_MS).coerceAtMost(1f)
                val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
                dropOffset = if (forward) drop * (1f - eased) else drop * eased
                if (progress >= 1f) break
            }
        } finally {
            displayedStage = target
            animatingStage = -1
            dropOffset = 0f
            lastSettledStage = target
        }
    }

    return StageAnimation(displayedStage, animatingStage, dropOffset)
}

private fun defaultCamera(subject: StructureSceneSubject): Scene3DCamera {
    val longest = max(8, max(subject.sizeX, subject.sizeZ))
    val zoom = (480f / longest).coerceIn(0.05f, 64f)
    return Scene3DCamera(
        yaw = 35f,
        pitch = 28f,
        zoom = zoom,
        panX = 0f,
        panY = subject.sizeY * zoom * 0.15f
    )
}
