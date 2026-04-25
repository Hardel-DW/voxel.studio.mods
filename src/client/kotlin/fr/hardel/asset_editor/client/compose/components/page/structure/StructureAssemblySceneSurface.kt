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
import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import net.minecraft.resources.Identifier

private val JIGSAW_ID = Identifier.withDefaultNamespace("jigsaw")

@Composable
fun StructureAssemblySceneSurface(
    state: Scene3DState,
    assembly: StructureAssemblySnapshot,
    displayedStep: Int,
    animatingPiece: Int,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    dropOffset: Float
) {
    val pendingCameras = remember(assembly.id()) { java.util.concurrent.ConcurrentHashMap<String, Scene3DCamera>() }

    DisposableEffect(assembly.id()) {
        val subscription = StructureSceneBridge.subscribe { key ->
            val image = StructureSceneBridge.getImage(key) ?: return@subscribe
            val camera = pendingCameras.remove(key) ?: state.camera
            state.publishFrame(key, image, camera)
        }
        onDispose(subscription::run)
    }

    LaunchedEffect(assembly.id(), displayedStep, animatingPiece, showJigsaws, sliceY, highlight, dropOffset) {
        snapshotFlow {
            val viewport = state.viewport
            if (viewport.width < 16 || viewport.height < 16) return@snapshotFlow null
            val camera = state.camera
            AssemblyRequest(
                key = assemblyKey(assembly, displayedStep, animatingPiece, showJigsaws, sliceY, highlight, camera, dropOffset, viewport),
                assembly = assembly,
                displayedStep = displayedStep,
                animatingPiece = animatingPiece,
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

private data class AssemblyRequest(
    val key: String,
    val assembly: StructureAssemblySnapshot,
    val displayedStep: Int,
    val animatingPiece: Int,
    val showJigsaws: Boolean,
    val sliceY: Int,
    val highlight: String?,
    val camera: Scene3DCamera,
    val drop: Float,
    val viewport: IntSize
)

private fun toRendererRequest(request: AssemblyRequest): StructureSceneRenderer.Request {
    val voxels = request.assembly.voxels()
        .asSequence()
        .filter { request.showJigsaws || it.blockId() != JIGSAW_ID }
        .filter { it.y() <= request.sliceY }
        .filter { it.pieceIndex() < request.displayedStep }
        .map { voxel ->
            val matchesSearch = request.highlight != null && voxel.blockId().toString().contains(request.highlight, ignoreCase = true)
            val isAnimating = voxel.pieceIndex() == request.animatingPiece
            StructureSceneRenderer.Voxel(
                voxel.blockId(),
                voxel.state(),
                voxel.x(),
                voxel.y(),
                voxel.z(),
                if (isAnimating) request.drop else 0f,
                matchesSearch
            )
        }
        .toList()
    return StructureSceneRenderer.Request(
        request.key,
        request.viewport.width,
        request.viewport.height,
        request.assembly.sizeX(),
        request.assembly.sizeY(),
        request.assembly.sizeZ(),
        voxels,
        StructureSceneRenderer.Camera(request.camera.yaw, request.camera.pitch, request.camera.zoom, request.camera.panX, request.camera.panY)
    )
}

private fun assemblyKey(
    assembly: StructureAssemblySnapshot,
    displayedStep: Int,
    animatingPiece: Int,
    showJigsaws: Boolean,
    sliceY: Int,
    highlight: String?,
    camera: Scene3DCamera,
    drop: Float,
    viewport: IntSize
): String = buildString {
    append("assembly|").append(assembly.id()).append('|')
    append(displayedStep).append('|')
    append(animatingPiece).append('|')
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
