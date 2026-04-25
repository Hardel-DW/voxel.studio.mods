package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import kotlin.math.max

private const val ANIM_DURATION_MS = 380f

@Composable
fun StructureAssemblySceneArea(
    assembly: StructureAssemblySnapshot,
    selectedStep: Int,
    animations: Boolean,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    modifier: Modifier = Modifier
) {
    val state = remember(assembly.id()) { Scene3DState(defaultCamera(assembly)) }
    var lastSettledStep by remember(assembly.id()) { mutableIntStateOf(selectedStep) }
    var displayedStep by remember(assembly.id()) { mutableIntStateOf(selectedStep) }
    var animatingPiece by remember(assembly.id()) { mutableIntStateOf(-1) }
    var dropOffset by remember(assembly.id()) { mutableStateOf(0f) }

    LaunchedEffect(selectedStep, animations, assembly.id()) {
        val target = selectedStep
        val current = lastSettledStep
        if (target == current) return@LaunchedEffect

        val forward = target > current
        val piece = if (forward) target - 1 else current - 1

        if (!animations || piece < 0) {
            displayedStep = target
            animatingPiece = -1
            dropOffset = 0f
            lastSettledStep = target
            return@LaunchedEffect
        }

        displayedStep = max(current, target)
        animatingPiece = piece
        val drop = max(assembly.sizeY(), 12).toFloat()

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
            displayedStep = target
            animatingPiece = -1
            dropOffset = 0f
            lastSettledStep = target
        }
    }

    LaunchedEffect(state) {
        StructureCameraReset.requests.collect {
            state.snapTo(defaultCamera(assembly))
        }
    }

    StructureAssemblySceneSurface(
        state = state,
        assembly = assembly,
        displayedStep = displayedStep,
        animatingPiece = animatingPiece,
        showJigsaws = showJigsaws,
        sliceY = sliceY,
        highlight = highlight,
        dropOffset = dropOffset
    )

    Scene3DCanvas(
        state = state,
        inputKey = assembly.id(),
        modifier = modifier,
        overlay = {
            IsometricGrid(
                state = state,
                bounds = GridBounds(assembly.sizeX(), assembly.sizeY(), assembly.sizeZ()),
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

private fun defaultCamera(assembly: StructureAssemblySnapshot): Scene3DCamera {
    val longest = max(8, max(assembly.sizeX(), assembly.sizeZ()))
    val zoom = (480f / longest).coerceIn(0.05f, 64f)
    return Scene3DCamera(
        yaw = 35f,
        pitch = 28f,
        zoom = zoom,
        panX = 0f,
        panY = assembly.sizeY() * zoom * 0.15f
    )
}
