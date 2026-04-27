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
import fr.hardel.asset_editor.network.structure.StructurePieceBox
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block

private val JIGSAW_ID = Identifier.withDefaultNamespace("jigsaw")

@Composable
fun StructureSceneSurface(
    state: Scene3DState,
    subject: StructureSceneSubject,
    filters: StructureSceneFilters,
    poolBoxes: List<StructurePieceBox>,
    showPoolBoxes: Boolean
) {
    val pendingCameras = remember(subject.id) { boundedCameraCache() }
    val latestKey = remember(subject.id) { java.util.concurrent.atomic.AtomicReference<String?>(null) }

    val voxels = remember(subject, filters.showJigsaws, filters.highlight) {
        buildVoxels(subject, filters)
    }

    val rendererBoxes = remember(poolBoxes) { buildPieceBoxes(poolBoxes) }

    val staticKey = remember(subject.id, filters.showJigsaws, filters.highlight, rendererBoxes) {
        buildStaticKey(subject, filters, rendererBoxes)
    }

    DisposableEffect(subject.id) {
        val subscription = StructureSceneBridge.subscribe { key ->
            if (key != latestKey.get()) return@subscribe
            val image = StructureSceneBridge.getImage(key) ?: return@subscribe
            val camera = pendingCameras.remove(key) ?: state.camera
            state.publishFrame(image, camera)
        }
        onDispose(subscription::run)
    }

    LaunchedEffect(subject.id, staticKey, filters.sliceY, filters.displayedStage, showPoolBoxes) {
        snapshotFlow {
            val viewport = state.viewport
            if (viewport.width < 16 || viewport.height < 16) return@snapshotFlow null
            buildRequest(subject, voxels, rendererBoxes, staticKey, filters, showPoolBoxes, state.camera, viewport)
        }
            .distinctUntilChanged { a, b -> a?.key() == b?.key() }
            .collectLatest { request ->
                if (request == null) return@collectLatest
                latestKey.set(request.key())
                val cached = StructureSceneBridge.getImage(request.key())
                if (cached != null) {
                    state.publishFrame(cached, request.cameraSnapshot())
                    return@collectLatest
                }
                pendingCameras[request.key()] = request.cameraSnapshot()
                StructureSceneBridge.request(request)
            }
    }
}

private fun StructureSceneRenderer.Request.cameraSnapshot(): Scene3DCamera =
    Scene3DCamera(camera().yaw(), camera().pitch(), camera().zoom(), camera().panX(), camera().panY())

private fun buildVoxels(subject: StructureSceneSubject, filters: StructureSceneFilters): List<StructureSceneRenderer.Voxel> {
    val out = ArrayList<StructureSceneRenderer.Voxel>()
    subject.forEachVoxel { blockId, blockStateId, x, y, z, stage, finalStateId ->
        var resolvedBlockId = blockId
        var resolvedStateId = blockStateId
        if (!filters.showJigsaws && blockId == JIGSAW_ID) {
            if (finalStateId == 0) return@forEachVoxel
            resolvedStateId = finalStateId
            resolvedBlockId = BuiltInRegistries.BLOCK.getKey(Block.stateById(finalStateId).block)
        }

        val highlighted = filters.highlight != null && resolvedBlockId.toString().contains(filters.highlight, ignoreCase = true)
        out.add(StructureSceneRenderer.Voxel(resolvedBlockId, resolvedStateId, x, y, z, stage, highlighted))
    }
    return out
}

private fun buildPieceBoxes(boxes: List<StructurePieceBox>): List<StructureSceneRenderer.PieceBox> =
    boxes.map { StructureSceneRenderer.PieceBox(it.pieceIndex(), it.minX(), it.minY(), it.minZ(), it.maxX(), it.maxY(), it.maxZ()) }

private fun buildRequest(
    subject: StructureSceneSubject,
    voxels: List<StructureSceneRenderer.Voxel>,
    pieceBoxes: List<StructureSceneRenderer.PieceBox>,
    staticKey: String,
    filters: StructureSceneFilters,
    showPoolBoxes: Boolean,
    camera: Scene3DCamera,
    viewport: IntSize
): StructureSceneRenderer.Request {
    val key = sceneKey(staticKey, filters.sliceY, filters.displayedStage, showPoolBoxes, camera, viewport)
    return StructureSceneRenderer.Request(
        key,
        staticKey,
        viewport.width,
        viewport.height,
        subject.sizeX,
        subject.sizeY,
        subject.sizeZ,
        voxels,
        pieceBoxes,
        filters.sliceY,
        filters.displayedStage,
        showPoolBoxes,
        StructureSceneRenderer.Camera(camera.yaw, camera.pitch, camera.zoom, camera.panX, camera.panY)
    )
}

private fun buildStaticKey(
    subject: StructureSceneSubject,
    filters: StructureSceneFilters,
    pieceBoxes: List<StructureSceneRenderer.PieceBox>
): String = buildString {
    append(if (subject is StructureSceneSubject.Assembly) "asm" else "tpl").append('|')
    append(subject.id).append('|')
    append(filters.showJigsaws).append('|')
    append(filters.highlight.orEmpty()).append('|')
    append('b').append(pieceBoxes.size)
}

private fun sceneKey(
    staticKey: String,
    sliceY: Int,
    displayedStage: Int,
    showPoolBoxes: Boolean,
    camera: Scene3DCamera,
    viewport: IntSize
): String = buildString {
    append(staticKey).append('|')
    append(sliceY).append('|')
    append(displayedStage).append('|')
    append(if (showPoolBoxes) 'p' else '_').append('|')
    append(viewport.width).append('x').append(viewport.height).append('|')
    append(encode(camera.yaw)).append(':')
    append(encode(camera.pitch)).append(':')
    append(encode(camera.zoom)).append(':')
    append(encode(camera.panX)).append(':')
    append(encode(camera.panY))
}

private fun encode(value: Float): Int = (value * 4f).toInt()

private fun boundedCameraCache(): MutableMap<String, Scene3DCamera> =
    object : java.util.LinkedHashMap<String, Scene3DCamera>(64, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Scene3DCamera>?): Boolean = size > 64
    }
