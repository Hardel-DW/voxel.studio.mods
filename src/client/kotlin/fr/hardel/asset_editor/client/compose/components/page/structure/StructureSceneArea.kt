package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.scene.GridBounds
import fr.hardel.asset_editor.client.compose.components.ui.scene.IsometricGrid
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCamera
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCanvas
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DState
import fr.hardel.asset_editor.network.structure.StructurePieceBox
import kotlin.math.max
import net.minecraft.resources.Identifier

@Composable
fun StructureSceneArea(
    subject: StructureSceneSubject,
    selectedStage: Int,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    modifier: Modifier = Modifier,
    showPoolBoxes: Boolean = false,
    onPieceSelected: ((Identifier) -> Unit)? = null
) {
    val state = remember(subject.id) { Scene3DState(defaultCamera(subject)) }
    val bounds = remember(subject.id, subject.sizeX, subject.sizeY, subject.sizeZ) {
        GridBounds(subject.sizeX, subject.sizeY, subject.sizeZ)
    }
    val pieceBoxes = pieceBoxesOf(subject)

    LaunchedEffect(state, subject.id) {
        StructureCameraReset.requests.collect {
            state.snapTo(defaultCamera(subject))
        }
    }

    val filters = StructureSceneFilters(
        displayedStage = if (subject.stageCount > 0) selectedStage else Int.MAX_VALUE,
        sliceY = sliceY,
        showJigsaws = showJigsaws,
        highlight = highlight
    )

    StructureSceneSurface(state = state, subject = subject, filters = filters)

    Scene3DCanvas(
        state = state,
        inputKey = subject.id,
        modifier = modifier,
        overlay = {
            IsometricGrid(state = state, bounds = bounds, modifier = Modifier.fillMaxSize())
        },
        foreground = if (showPoolBoxes && pieceBoxes.isNotEmpty()) {
            { StructurePoolBoxes(state = state, boxes = pieceBoxes, bounds = bounds, modifier = Modifier.fillMaxSize()) }
        } else null,
        onClick = if (showPoolBoxes && pieceBoxes.isNotEmpty() && onPieceSelected != null) {
            { offset -> pickPieceBoxAt(state, pieceBoxes, bounds, offset)?.let { onPieceSelected(it.templateId()) } }
        } else null
    )
}

private fun pieceBoxesOf(subject: StructureSceneSubject): List<StructurePieceBox> = when (subject) {
    is StructureSceneSubject.Assembly -> subject.assembly.pieceBoxes()
    is StructureSceneSubject.Template -> emptyList()
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
