package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.IntSize
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCamera
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DState
import fr.hardel.asset_editor.client.compose.lib.StructureSceneBridge
import fr.hardel.asset_editor.client.rendering.StructureSceneRenderer
import fr.hardel.asset_editor.network.structure.StructureBlockVoxel
import fr.hardel.asset_editor.network.structure.StructureJigsawNode
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import net.minecraft.resources.Identifier
import kotlin.math.abs

private val JIGSAW_ID = Identifier.withDefaultNamespace("jigsaw")

@Composable
fun StructureSceneSurface(
    state: Scene3DState,
    template: StructureTemplateSnapshot,
    viewMode: StructureViewMode,
    selectedStep: Int,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    dropOffset: Float
) {
    val pendingCameras = remember(template.id()) { boundedCameraCache() }

    DisposableEffect(template.id()) {
        val subscription = StructureSceneBridge.subscribe { key ->
            val image = StructureSceneBridge.getImage(key) ?: return@subscribe
            val camera = pendingCameras.remove(key) ?: state.camera
            state.publishFrame(key, image, camera)
        }
        onDispose(subscription::run)
    }

    LaunchedEffect(template.id(), viewMode, selectedStep, showJigsaws, sliceY, highlight, dropOffset) {
        snapshotFlow {
            val viewport = state.viewport
            if (viewport.width < 16 || viewport.height < 16) return@snapshotFlow null
            val camera = state.camera
            SceneRenderRequest(
                key = sceneKey(template, viewMode, selectedStep, showJigsaws, sliceY, highlight, camera, dropOffset, viewport),
                template = template,
                viewMode = viewMode,
                selectedStep = selectedStep,
                showJigsaws = showJigsaws,
                sliceY = sliceY,
                highlight = highlight,
                camera = camera,
                drop = dropOffset,
                viewport = viewport
            )
        }
            .distinctUntilChanged { a, b -> a?.key == b?.key }
            .collectLatest { request ->
                if (request == null) return@collectLatest
                val cached = StructureSceneBridge.getImage(request.key)
                if (cached != null) {
                    state.publishFrame(request.key, cached, request.camera)
                    return@collectLatest
                }
                pendingCameras[request.key] = request.camera
                StructureSceneBridge.request(toRendererRequest(request))
            }
    }
}

private data class SceneRenderRequest(
    val key: String,
    val template: StructureTemplateSnapshot,
    val viewMode: StructureViewMode,
    val selectedStep: Int,
    val showJigsaws: Boolean,
    val sliceY: Int,
    val highlight: String?,
    val camera: Scene3DCamera,
    val drop: Float,
    val viewport: IntSize
)

private fun toRendererRequest(request: SceneRenderRequest): StructureSceneRenderer.Request {
    val activeJigsaw = if (request.viewMode == StructureViewMode.STRUCTURE)
        request.template.jigsaws().getOrNull(request.selectedStep - 1)
    else null
    val voxels = request.template.voxels()
        .asSequence()
        .filter { request.showJigsaws || it.blockId() != JIGSAW_ID }
        .filter { it.y() <= request.sliceY }
        .map { voxel ->
            val activePiece = activeJigsaw != null && distance(voxel, activeJigsaw) <= 5
            val highlighted = request.highlight != null && voxel.blockId().toString().contains(request.highlight, ignoreCase = true)
            StructureSceneRenderer.Voxel(
                voxel.blockId(),
                voxel.state(),
                voxel.x(),
                voxel.y(),
                voxel.z(),
                if (activePiece) request.drop else 0f,
                highlighted
            )
        }
        .toList()
    return StructureSceneRenderer.Request(
        request.key,
        sceneOnlyKey(request.template, request.viewMode, request.selectedStep, request.showJigsaws, request.sliceY, request.highlight, request.drop),
        request.viewport.width,
        request.viewport.height,
        request.template.sizeX(),
        request.template.sizeY(),
        request.template.sizeZ(),
        voxels,
        StructureSceneRenderer.Camera(request.camera.yaw, request.camera.pitch, request.camera.zoom, request.camera.panX, request.camera.panY)
    )
}

private fun sceneOnlyKey(
    template: StructureTemplateSnapshot,
    viewMode: StructureViewMode,
    selectedStep: Int,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    drop: Float
): String = buildString {
    append(template.id()).append('|')
    append(viewMode.id).append('|')
    append(selectedStep).append('|')
    append(showJigsaws).append('|')
    append(sliceY).append('|')
    append(highlight.orEmpty()).append('|')
    append(encode(drop))
}

private fun sceneKey(
    template: StructureTemplateSnapshot,
    viewMode: StructureViewMode,
    selectedStep: Int,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    camera: Scene3DCamera,
    drop: Float,
    viewport: IntSize
): String = buildString {
    append(template.id()).append('|')
    append(viewMode.id).append('|')
    append(selectedStep).append('|')
    append(showJigsaws).append('|')
    append(sliceY).append('|')
    append(highlight.orEmpty()).append('|')
    append(viewport.width).append('x').append(viewport.height).append('|')
    append(encode(camera.yaw)).append(':')
    append(encode(camera.pitch)).append(':')
    append(encode(camera.zoom)).append(':')
    append(encode(camera.panX)).append(':')
    append(encode(camera.panY)).append('|')
    append(encode(drop))
}

private fun encode(value: Float): Int = (value * 4f).toInt()

private fun boundedCameraCache(): MutableMap<String, Scene3DCamera> {
    val map = object : java.util.LinkedHashMap<String, Scene3DCamera>(64, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Scene3DCamera>?): Boolean = size > 64
    }
    return java.util.Collections.synchronizedMap(map)
}

private fun distance(voxel: StructureBlockVoxel, jigsaw: StructureJigsawNode): Int =
    abs(voxel.x() - jigsaw.x()) + abs(voxel.y() - jigsaw.y()) + abs(voxel.z() - jigsaw.z())
