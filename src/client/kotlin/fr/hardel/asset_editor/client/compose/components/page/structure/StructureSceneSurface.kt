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
import net.minecraft.resources.Identifier

private val JIGSAW_ID = Identifier.withDefaultNamespace("jigsaw")

@Composable
fun StructureSceneSurface(
    state: Scene3DState,
    subject: StructureSceneSubject,
    filters: StructureSceneFilters
) {
    val pendingCameras = remember(subject.id) { boundedCameraCache() }

    val voxels = remember(
        subject,
        filters.showJigsaws,
        filters.highlight,
        filters.displayedStage,
        filters.animatingStage
    ) {
        buildVoxels(subject, filters)
    }

    val staticKey = remember(
        subject.id,
        filters.showJigsaws,
        filters.highlight,
        filters.displayedStage,
        filters.animatingStage
    ) {
        buildStaticKey(subject, filters)
    }

    val animatingKey = remember(
        subject.id,
        filters.showJigsaws,
        filters.highlight,
        filters.animatingStage
    ) {
        if (filters.animatingStage < 0) "" else buildAnimatingKey(subject, filters)
    }

    DisposableEffect(subject.id) {
        val subscription = StructureSceneBridge.subscribe { key ->
            val image = StructureSceneBridge.getImage(key) ?: return@subscribe
            val camera = pendingCameras.remove(key) ?: state.camera
            state.publishFrame(image, camera)
        }
        onDispose(subscription::run)
    }

    LaunchedEffect(subject.id, staticKey, animatingKey, filters.sliceY, filters.drop) {
        snapshotFlow {
            val viewport = state.viewport
            if (viewport.width < 16 || viewport.height < 16) return@snapshotFlow null
            buildRequest(subject, voxels, staticKey, animatingKey, filters, state.camera, viewport)
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
    subject.forEachVoxel { blockId, blockStateId, x, y, z, stage ->
        if (stage >= filters.displayedStage) return@forEachVoxel
        if (!filters.showJigsaws && blockId == JIGSAW_ID) return@forEachVoxel
        val animating = stage == filters.animatingStage
        val highlighted = filters.highlight != null && blockId.toString().contains(filters.highlight, ignoreCase = true)
        out.add(StructureSceneRenderer.Voxel(blockId, blockStateId, x, y, z, animating, highlighted))
    }
    return out
}

private fun buildRequest(
    subject: StructureSceneSubject,
    voxels: List<StructureSceneRenderer.Voxel>,
    staticKey: String,
    animatingKey: String,
    filters: StructureSceneFilters,
    camera: Scene3DCamera,
    viewport: IntSize
): StructureSceneRenderer.Request {
    val key = sceneKey(staticKey, animatingKey, filters.sliceY, filters.drop, camera, viewport)
    return StructureSceneRenderer.Request(
        key,
        staticKey,
        animatingKey,
        viewport.width,
        viewport.height,
        subject.sizeX,
        subject.sizeY,
        subject.sizeZ,
        voxels,
        filters.sliceY,
        filters.drop,
        StructureSceneRenderer.Camera(camera.yaw, camera.pitch, camera.zoom, camera.panX, camera.panY)
    )
}

private fun buildStaticKey(subject: StructureSceneSubject, filters: StructureSceneFilters): String = buildString {
    append(if (subject is StructureSceneSubject.Assembly) "asm" else "tpl").append('|')
    append(subject.id).append('|')
    append(canonicalDisplayedStage(filters.displayedStage, filters.animatingStage)).append('|')
    append(filters.animatingStage).append('|')
    append(filters.showJigsaws).append('|')
    append(filters.highlight.orEmpty())
}

private fun buildAnimatingKey(subject: StructureSceneSubject, filters: StructureSceneFilters): String = buildString {
    append("anim|").append(subject.id).append('|')
    append(filters.animatingStage).append('|')
    append(filters.showJigsaws).append('|')
    append(filters.highlight.orEmpty())
}

private fun canonicalDisplayedStage(displayedStage: Int, animatingStage: Int): Int =
    if (animatingStage in 0 until displayedStage && animatingStage == displayedStage - 1)
        displayedStage - 1
    else
        displayedStage

private fun sceneKey(
    staticKey: String,
    animatingKey: String,
    sliceY: Int,
    drop: Float,
    camera: Scene3DCamera,
    viewport: IntSize
): String = buildString {
    append(staticKey).append('|')
    append(animatingKey).append('|')
    append(sliceY).append('|')
    append(viewport.width).append('x').append(viewport.height).append('|')
    append(encode(camera.yaw)).append(':')
    append(encode(camera.pitch)).append(':')
    append(encode(camera.zoom)).append(':')
    append(encode(camera.panX)).append(':')
    append(encode(camera.panY)).append('|')
    append(encode(drop))
}

private fun encode(value: Float): Int = (value * 4f).toInt()

private fun boundedCameraCache(): MutableMap<String, Scene3DCamera> =
    object : java.util.LinkedHashMap<String, Scene3DCamera>(64, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Scene3DCamera>?): Boolean = size > 64
    }
