package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCamera
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DState
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DStateMemory
import kotlin.math.max
import net.minecraft.resources.Identifier

/**
 * Wraps [StructureSceneArea] with the standard overlays (top title bar slot, bottom controls, Y slider)
 * and owns the locally-scoped UI state (slice Y, jigsaws, current stage, pool boxes).
 *
 * Stage controls and the pool-boxes toggle appear automatically when the subject has stages.
 * The caller passes in the [Scene3DState] so it can observe first-frame readiness for cross-cutting
 * loading states (e.g. holding a side panel until the scene is rendered).
 */
@Composable
fun StructureSceneScaffold(
    state: Scene3DState,
    subject: StructureSceneSubject,
    initialShowJigsaws: Boolean,
    modifier: Modifier = Modifier,
    highlight: String? = null,
    onSelectPiece: ((Identifier) -> Unit)? = null,
    topOverlay: @Composable BoxScope.() -> Unit
) {
    val showStageControls = subject.stageCount > 0
    val maxStep = subject.stageCount
    var step by remember(subject.id) { mutableIntStateOf(maxStep) }
    var showJigsaws by remember { mutableStateOf(initialShowJigsaws) }
    var showPoolBoxes by remember { mutableStateOf(false) }
    var sliceY by remember(subject.id) { mutableIntStateOf(subject.sizeY) }

    Box(modifier) {
        StructureSceneArea(
            state = state,
            subject = subject,
            selectedStage = if (showStageControls) step.coerceIn(0, maxStep) else 0,
            showJigsaws = showJigsaws,
            sliceY = sliceY,
            highlight = highlight,
            modifier = Modifier.fillMaxSize(),
            showPoolBoxes = showPoolBoxes && showStageControls,
            onPieceSelected = onSelectPiece
        )
        topOverlay()
        StructureBottomOverlay(
            showStageControls = showStageControls,
            step = step,
            maxStep = maxStep,
            onStepChange = { step = it.coerceIn(0, maxStep) },
            showJigsaws = showJigsaws,
            onShowJigsawsChange = { showJigsaws = it },
            showPoolToggle = showStageControls,
            showPoolBoxes = showPoolBoxes,
            onShowPoolBoxesChange = { showPoolBoxes = it },
            zoomOnCursor = StructureUiState.zoomOnCursor,
            onZoomOnCursorChange = { StructureUiState.zoomOnCursor = it },
            onReset = { StructureCameraReset.requestReset() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        StructureYSlider(
            value = sliceY,
            max = subject.sizeY,
            onValueChange = { sliceY = it },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )
    }
}

@Composable
fun rememberStructureSceneState(subject: StructureSceneSubject): Scene3DState =
    remember(subject.id) {
        Scene3DStateMemory.obtain(subject.id.toString()) { defaultCameraFor(subject) }
    }

internal fun defaultCameraFor(subject: StructureSceneSubject): Scene3DCamera {
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

internal fun Scene3DState.snapToDefaultFor(subject: StructureSceneSubject) {
    snapTo(defaultCameraFor(subject))
}
