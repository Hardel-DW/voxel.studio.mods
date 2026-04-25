package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.scene.GridBounds
import fr.hardel.asset_editor.client.compose.components.ui.scene.IsometricGrid
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCamera
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCanvas
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DState
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import kotlin.math.max

@Composable
fun StructureSceneArea(
    template: StructureTemplateSnapshot,
    viewMode: StructureViewMode,
    selectedStep: Int,
    animations: Boolean,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    modifier: Modifier = Modifier
) {
    val state = remember(template.id()) { Scene3DState(defaultCamera(template)) }
    var dropOffset by remember(template.id()) { mutableStateOf(0f) }

    LaunchedEffect(template.id(), selectedStep, viewMode, animations) {
        if (animations && viewMode == StructureViewMode.STRUCTURE && selectedStep > 0) {
            val drop = max(template.sizeY(), 12).toFloat()
            val durationMillis = 380f
            val startNanos = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val elapsed = ((now - startNanos) / 1_000_000f).coerceAtLeast(0f)
                val progress = (elapsed / durationMillis).coerceAtMost(1f)
                val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
                dropOffset = drop * (1f - eased)
                if (progress >= 1f) {
                    dropOffset = 0f
                    break
                }
            }
        } else {
            dropOffset = 0f
        }
    }

    LaunchedEffect(state) {
        StructureCameraReset.requests.collect {
            state.snapTo(defaultCamera(template))
        }
    }

    StructureSceneSurface(
        state = state,
        template = template,
        viewMode = viewMode,
        selectedStep = selectedStep,
        showJigsaws = showJigsaws,
        sliceY = sliceY,
        highlight = highlight,
        dropOffset = dropOffset
    )

    Scene3DCanvas(
        state = state,
        inputKey = template.id(),
        modifier = modifier,
        overlay = {
            IsometricGrid(
                state = state,
                bounds = GridBounds(template.sizeX(), template.sizeY(), template.sizeZ()),
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

private fun defaultCamera(template: StructureTemplateSnapshot): Scene3DCamera {
    val longest = max(8, max(template.sizeX(), template.sizeZ()))
    val zoom = (480f / longest).coerceIn(0.4f, 64f)
    return Scene3DCamera(
        yaw = 35f,
        pitch = 28f,
        zoom = zoom,
        panX = 0f,
        panY = template.sizeY() * zoom * 0.15f
    )
}
