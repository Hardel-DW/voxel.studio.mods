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
    filters: StructureSceneFilters
) {
    val pendingCameras = remember(subject.id) { boundedCameraCache() }

    val voxels = remember(subject, filters.showJigsaws, filters.highlight) {
        buildVoxels(subject, filters)
    }

    val staticKey = remember(subject, filters.showJigsaws, filters.highlight) {
        buildStaticKey(subject, filters)
    }

    DisposableEffect(subject.id) {
        val subscription = StructureSceneBridge.subscribe { key ->
            val image = StructureSceneBridge.getImage(key) ?: return@subscribe
            val camera = pendingCameras.remove(key) ?: state.camera
            state.publishFrame(image, camera)
        }
        onDispose(subscription::run)
    }

    LaunchedEffect(subject.id, staticKey, filters.sliceY, filters.displayedStage) {
        snapshotFlow {
            val viewport = state.viewport
            if (viewport.width < 16 || viewport.height < 16) return@snapshotFlow null
            buildRequest(subject, voxels, staticKey, filters, state.camera, viewport)
        }
            .distinctUntilChanged { a, b -> a?.key() == b?.key() }
            .collectLatest { request ->
                if (request == null) return@collectLatest
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

private fun buildRequest(
    subject: StructureSceneSubject,
    voxels: List<StructureSceneRenderer.Voxel>,
    staticKey: String,
    filters: StructureSceneFilters,
    camera: Scene3DCamera,
    viewport: IntSize
): StructureSceneRenderer.Request {
    val key = sceneKey(staticKey, filters.sliceY, filters.displayedStage, camera, viewport)
    return StructureSceneRenderer.Request(
        key,
        staticKey,
        viewport.width,
        viewport.height,
        subject.sizeX,
        subject.sizeY,
        subject.sizeZ,
        voxels,
        filters.sliceY,
        filters.displayedStage,
        StructureSceneRenderer.Camera(camera.yaw, camera.pitch, camera.zoom, camera.panX, camera.panY)
    )
}

private fun buildStaticKey(subject: StructureSceneSubject, filters: StructureSceneFilters): String = buildString {
    append(if (subject is StructureSceneSubject.Assembly) "asm" else "tpl").append('|')
    append(subject.id).append('|')
    if (subject is StructureSceneSubject.Assembly) {
        val params = subject.assembly.parameters()
        append(params.seed()).append(':')
        append(params.chunkX()).append(':')
        append(params.chunkZ()).append('|')
    }
    append(filters.showJigsaws).append('|')
    append(filters.highlight.orEmpty())
}

private fun sceneKey(
    staticKey: String,
    sliceY: Int,
    displayedStage: Int,
    camera: Scene3DCamera,
    viewport: IntSize
): String = buildString {
    append(staticKey).append('|')
    append(sliceY).append('|')
    append(displayedStage).append('|')
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
