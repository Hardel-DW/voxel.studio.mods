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
import net.minecraft.resources.Identifier

/**
 * Wraps [StructureSceneArea] with the standard overlays (top title bar slot, bottom controls, Y slider)
 * and owns the locally-scoped UI state (slice Y, jigsaws, current stage, animations, pool boxes).
 * Stage controls and the pool-boxes toggle appear automatically when the subject has stages
 * (i.e. it is an Assembly).
 */
@Composable
fun StructureSceneScaffold(
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
    var animations by remember { mutableStateOf(true) }
    var showJigsaws by remember { mutableStateOf(initialShowJigsaws) }
    var showPoolBoxes by remember { mutableStateOf(false) }
    var sliceY by remember(subject.id) { mutableIntStateOf(subject.sizeY) }

    Box(modifier) {
        StructureSceneArea(
            subject = subject,
            selectedStage = if (showStageControls) step.coerceIn(0, maxStep) else 0,
            animations = animations && showStageControls,
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
            animations = animations,
            onAnimationsChange = { animations = it },
            showJigsaws = showJigsaws,
            onShowJigsawsChange = { showJigsaws = it },
            showPoolToggle = showStageControls,
            showPoolBoxes = showPoolBoxes,
            onShowPoolBoxesChange = { showPoolBoxes = it },
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
