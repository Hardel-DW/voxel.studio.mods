package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.ToggleSwitch
import fr.hardel.asset_editor.client.compose.lib.StructureSceneBridge
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.client.rendering.StructureSceneRenderer
import fr.hardel.asset_editor.network.structure.StructureBlockCount
import fr.hardel.asset_editor.network.structure.StructureBlockVoxel
import fr.hardel.asset_editor.network.structure.StructureJigsawNode
import fr.hardel.asset_editor.network.structure.StructureReplaceBlocksPayload
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import net.minecraft.resources.Identifier
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.exp
import kotlin.math.roundToInt

private val JIGSAW_ID = Identifier.withDefaultNamespace("jigsaw")

@Composable
fun StructureMainPage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context) ?: return
    val templates = rememberServerData(StudioDataSlots.STRUCTURE_TEMPLATES)
    val template = remember(templates, destination.elementId) {
        templates.firstOrNull { it.id().toString() == destination.elementId }
    } ?: return

    var selectedStep by remember(template.id()) { mutableIntStateOf(0) }
    var animations by remember { mutableStateOf(true) }
    var showJigsaws by remember { mutableStateOf(true) }
    var blockQuery by remember { mutableStateOf("") }
    var fromBlock by remember { mutableStateOf("") }
    var toBlock by remember { mutableStateOf("") }
    val maxStep = template.jigsaws().size
    selectedStep = selectedStep.coerceIn(0, maxStep)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StructureViewerHeader(template)
            StructureSceneViewer(
                template = template,
                selectedStep = selectedStep,
                animations = animations,
                showJigsaws = showJigsaws,
                highlightedBlock = fromBlock.takeIf { it.isNotBlank() } ?: blockQuery.takeIf { it.isNotBlank() },
                modifier = Modifier.weight(1f)
            )
            StructureTimeline(
                template = template,
                selectedStep = selectedStep,
                onStepChange = { selectedStep = it.coerceIn(0, maxStep) },
                animations = animations,
                onAnimationsChange = { animations = it },
                showJigsaws = showJigsaws,
                onShowJigsawsChange = { showJigsaws = it }
            )
        }

        StructureInspector(
            context = context,
            template = template,
            query = blockQuery,
            onQueryChange = { blockQuery = it },
            fromBlock = fromBlock,
            onFromBlockChange = { fromBlock = it },
            toBlock = toBlock,
            onToBlockChange = { toBlock = it }
        )
    }
}

@Composable
private fun StructureViewerHeader(template: StructureTemplateSnapshot) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(template.id().toString(), style = StudioTypography.semiBold(22), color = StudioColors.Zinc100)
            Text(template.sourcePack(), style = StudioTypography.regular(12), color = StudioColors.Zinc500)
        }
        HeaderMetric("${template.sizeX()}x${template.sizeY()}x${template.sizeZ()}", "taille")
        HeaderMetric(template.totalBlocks().toString(), "blocs")
        HeaderMetric(template.entityCount().toString(), "entites")
    }
}

@Composable
private fun HeaderMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(92.dp)) {
        Text(value, style = StudioTypography.semiBold(14), color = StudioColors.Zinc100)
        Text(label, style = StudioTypography.regular(10), color = StudioColors.Zinc500)
    }
}

@Composable
private fun StructureSceneViewer(
    template: StructureTemplateSnapshot,
    selectedStep: Int,
    animations: Boolean,
    showJigsaws: Boolean,
    highlightedBlock: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val focusRequester = remember { FocusRequester() }
    val sceneState = remember(template.id()) { StructureSceneState(defaultCamera(template)) }
    val drop = remember(template.id()) { Animatable(0f) }
    val quantizedDrop = (drop.value * 4f).roundToInt() / 4f

    LaunchedEffect(template.id()) {
        runCatching { focusRequester.requestFocus() }
    }
    LaunchedEffect(sceneState) {
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val deltaSeconds = ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f)
            previous = now
            sceneState.stepMomentum(deltaSeconds)
        }
    }
    LaunchedEffect(template.id(), selectedStep, animations) {
        if (animations && selectedStep > 0) {
            drop.snapTo(-max(template.sizeY(), 10).toFloat())
            drop.animateTo(0f, tween(StudioMotion.Medium4, easing = StudioMotion.EmphasizedDecelerate))
        } else {
            drop.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioColors.Zinc925, shape)
            .border(1.dp, StudioColors.Zinc800, shape)
            .clip(shape)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val step = 42f
                when (event.key) {
                    Key.Z, Key.DirectionUp -> sceneState.pan(0f, -step, 0.035f)
                    Key.S, Key.DirectionDown -> sceneState.pan(0f, step, 0.035f)
                    Key.Q, Key.DirectionLeft -> sceneState.pan(-step, 0f, 0.035f)
                    Key.D, Key.DirectionRight -> sceneState.pan(step, 0f, 0.035f)
                    Key.A -> sceneState.rotate(-8f, 0f, 0.035f)
                    Key.E -> sceneState.rotate(8f, 0f, 0.035f)
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var lastPosition: Offset? = null
                    var lastTimeMillis = 0L
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val change = event.changes.firstOrNull() ?: continue
                            val factor = if (change.scrollDelta.y > 0f) 0.88f else 1.12f
                            sceneState.zoom(factor, change.position)
                            change.consume()
                            continue
                        }

                        val change = event.changes.firstOrNull() ?: continue
                        if (!change.pressed) {
                            lastPosition = null
                            sceneState.dragging = false
                            continue
                        }
                        sceneState.dragging = true
                        runCatching { focusRequester.requestFocus() }
                        val previous = lastPosition
                        val current = change.position
                        if (previous != null) {
                            val delta = current - previous
                            val deltaSeconds = ((change.uptimeMillis - lastTimeMillis) / 1000f).coerceAtLeast(0.001f)
                            sceneState.rotate(delta.x * 0.32f, delta.y * 0.24f, deltaSeconds)
                            change.consume()
                        }
                        lastPosition = current
                        lastTimeMillis = change.uptimeMillis
                    }
                }
            }
    ) {
        StructureSceneSurface(
            state = sceneState,
            template = template,
            selectedStep = selectedStep,
            showJigsaws = showJigsaws,
            highlightedBlock = highlightedBlock,
            drop = quantizedDrop,
            modifier = Modifier.fillMaxSize().padding(10.dp)
        )

        if (sceneState.frame == null) {
            Text(
                "Rendu Minecraft...",
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { sceneState.reset(defaultCamera(template)) }, variant = ButtonVariant.GHOST_BORDER, size = ButtonSize.SM, text = "Reset")
        }

    }
}

@Composable
private fun StructureSceneSurface(
    state: StructureSceneState,
    template: StructureTemplateSnapshot,
    selectedStep: Int,
    showJigsaws: Boolean,
    highlightedBlock: String?,
    drop: Float,
    modifier: Modifier = Modifier
) {
    val pendingFrames = remember(template.id()) { HashMap<String, StructureRenderDescriptor>() }

    DisposableEffect(template.id()) {
        val subscription = StructureSceneBridge.subscribe { completed ->
            val descriptor = pendingFrames.remove(completed) ?: return@subscribe
            if (descriptor.serial < state.appliedRenderSerial) return@subscribe
            val image = StructureSceneBridge.getImage(completed) ?: return@subscribe
            state.appliedRenderSerial = descriptor.serial
            state.frame = StructureSceneFrame(completed, image, descriptor.camera, descriptor.viewport)
        }
        onDispose(subscription::run)
    }

    LaunchedEffect(template.id(), selectedStep, showJigsaws, highlightedBlock, drop) {
        snapshotFlow {
            val viewport = state.viewport
            if (viewport.width < 16 || viewport.height < 16) {
                null
            } else {
                val camera = state.camera
                StructureRenderDescriptor(
                    sceneKey(template, selectedStep, showJigsaws, highlightedBlock, camera, drop, viewport),
                    camera,
                    viewport
                )
            }
        }
            .distinctUntilChanged()
            .collectLatest { descriptor ->
                if (descriptor == null) return@collectLatest
                val requestDescriptor = descriptor.copy(serial = state.nextRenderSerial())
                state.activeRenderKey = requestDescriptor.key
                val cached = StructureSceneBridge.getImage(descriptor.key)
                if (cached != null) {
                    state.appliedRenderSerial = requestDescriptor.serial
                    state.frame = StructureSceneFrame(requestDescriptor.key, cached, requestDescriptor.camera, requestDescriptor.viewport)
                    return@collectLatest
                }
                pendingFrames[requestDescriptor.key] = requestDescriptor
                StructureSceneBridge.request(buildSceneRequest(template, selectedStep, showJigsaws, highlightedBlock, requestDescriptor.camera, drop, requestDescriptor.viewport, requestDescriptor.key))
            }
    }

    Box(modifier = modifier.clipToBounds().onSizeChanged { state.viewport = it }) {
        val frame = state.frame ?: return@Box
        Image(
            bitmap = frame.image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val camera = state.camera
                    val rendered = frame.camera
                    val scale = camera.zoom / rendered.zoom
                    scaleX = scale
                    scaleY = scale
                    translationX = camera.panX - rendered.panX * scale
                    translationY = camera.panY - rendered.panY * scale
                }
        )
    }
}

private fun buildSceneRequest(
    template: StructureTemplateSnapshot,
    selectedStep: Int,
    showJigsaws: Boolean,
    highlightedBlock: String?,
    camera: StructureCamera,
    drop: Float,
    viewport: IntSize,
    key: String
): StructureSceneRenderer.Request {
    val activeJigsaw = template.jigsaws().getOrNull(selectedStep - 1)
    val voxels = template.voxels()
        .asSequence()
        .filter { showJigsaws || it.blockId() != JIGSAW_ID }
        .map { voxel ->
            val activePiece = activeJigsaw != null && distance(voxel, activeJigsaw) <= 5
            val highlighted = highlightedBlock?.let { voxel.blockId().toString().contains(it, ignoreCase = true) } == true
            StructureSceneRenderer.Voxel(
                voxel.blockId(),
                voxel.state(),
                voxel.x(),
                voxel.y(),
                voxel.z(),
                if (activePiece) drop else 0f,
                highlighted
            )
        }
        .toList()
    return StructureSceneRenderer.Request(
        key,
        viewport.width,
        viewport.height,
        template.sizeX(),
        template.sizeY(),
        template.sizeZ(),
        voxels,
        StructureSceneRenderer.Camera(camera.yaw, camera.pitch, camera.zoom, camera.panX, camera.panY)
    )
}

private fun sceneKey(
    template: StructureTemplateSnapshot,
    selectedStep: Int,
    showJigsaws: Boolean,
    highlightedBlock: String?,
    camera: StructureCamera,
    drop: Float,
    viewport: IntSize
): String = buildString {
    append(template.id()).append('|')
    append(template.voxels().size).append('|')
    append(viewport.width).append('x').append(viewport.height).append('|')
    append(selectedStep).append('|')
    append(showJigsaws).append('|')
    append(highlightedBlock.orEmpty()).append('|')
    append(camera.yaw.roundToInt()).append('|')
    append(camera.pitch.roundToInt()).append('|')
    append(camera.zoom.roundToInt()).append('|')
    append(camera.panX.roundToInt()).append('|')
    append(camera.panY.roundToInt()).append('|')
    append((drop * 100f).roundToInt())
}

private data class StructureCamera(
    val yaw: Float,
    val pitch: Float,
    val zoom: Float,
    val panX: Float,
    val panY: Float
)

private data class StructureRenderDescriptor(
    val key: String,
    val camera: StructureCamera,
    val viewport: IntSize,
    val serial: Long = 0L
)

private data class StructureSceneFrame(
    val key: String,
    val image: ImageBitmap,
    val camera: StructureCamera,
    val viewport: IntSize
)

@Stable
private class StructureSceneState(initialCamera: StructureCamera) {
    var camera by mutableStateOf(initialCamera)
    var viewport by mutableStateOf(IntSize.Zero)
    var frame by mutableStateOf<StructureSceneFrame?>(null)
    var activeRenderKey: String = ""
    var appliedRenderSerial: Long = -1L
    var dragging: Boolean = false
    private var renderSerial: Long = 0L
    private var panVelocityX: Float = 0f
    private var panVelocityY: Float = 0f
    private var yawVelocity: Float = 0f
    private var pitchVelocity: Float = 0f
    private var zoomVelocity: Float = 0f

    fun reset(defaultCamera: StructureCamera) {
        camera = defaultCamera
        panVelocityX = 0f
        panVelocityY = 0f
        yawVelocity = 0f
        pitchVelocity = 0f
        zoomVelocity = 0f
    }

    fun nextRenderSerial(): Long = ++renderSerial

    fun pan(deltaX: Float, deltaY: Float, deltaSeconds: Float) {
        camera = camera.copy(panX = camera.panX + deltaX, panY = camera.panY + deltaY)
        panVelocityX = deltaX / deltaSeconds
        panVelocityY = deltaY / deltaSeconds
    }

    fun rotate(deltaYaw: Float, deltaPitch: Float, deltaSeconds: Float) {
        camera = camera.copy(
            yaw = camera.yaw + deltaYaw,
            pitch = (camera.pitch + deltaPitch).coerceIn(-78f, 78f)
        )
        yawVelocity = deltaYaw / deltaSeconds
        pitchVelocity = deltaPitch / deltaSeconds
    }

    fun zoom(factor: Float, anchor: Offset) {
        val current = camera
        val clampedZoom = (current.zoom * factor).coerceAtLeast(0.05f)
        val appliedFactor = clampedZoom / current.zoom
        val localX = anchor.x - viewport.width * 0.5f
        val localY = anchor.y - viewport.height * 0.5f
        camera = current.copy(
            zoom = clampedZoom,
            panX = localX - appliedFactor * (localX - current.panX),
            panY = localY - appliedFactor * (localY - current.panY)
        )
        zoomVelocity += if (factor > 1f) 2.2f else -2.2f
    }

    fun stepMomentum(deltaSeconds: Float) {
        if (dragging) return
        val hasPan = abs(panVelocityX) > 1f || abs(panVelocityY) > 1f
        val hasRotation = abs(yawVelocity) > 1f || abs(pitchVelocity) > 1f
        val hasZoom = abs(zoomVelocity) > 0.01f
        if (!hasPan && !hasRotation && !hasZoom) return

        var next = camera
        if (hasPan) {
            next = next.copy(
                panX = next.panX + panVelocityX * deltaSeconds,
                panY = next.panY + panVelocityY * deltaSeconds
            )
        }
        if (hasRotation) {
            next = next.copy(
                yaw = next.yaw + yawVelocity * deltaSeconds,
                pitch = (next.pitch + pitchVelocity * deltaSeconds).coerceIn(-78f, 78f)
            )
        }
        if (hasZoom) {
            next = next.copy(zoom = (next.zoom * exp(zoomVelocity * deltaSeconds)).coerceAtLeast(0.05f))
        }
        camera = next

        val damping = exp(-10f * deltaSeconds)
        panVelocityX *= damping
        panVelocityY *= damping
        yawVelocity *= damping
        pitchVelocity *= damping
        zoomVelocity *= damping
    }
}

private fun defaultCamera(template: StructureTemplateSnapshot): StructureCamera {
    val longest = max(8, max(template.sizeX(), template.sizeZ()))
    val zoom = (520f / longest).coerceAtLeast(0.05f)
    return StructureCamera(yaw = 45f, pitch = 32f, zoom = zoom, panX = 0f, panY = template.sizeY() * zoom * 0.18f)
}

@Composable
private fun StructureTimeline(
    template: StructureTemplateSnapshot,
    selectedStep: Int,
    onStepChange: (Int) -> Unit,
    animations: Boolean,
    onAnimationsChange: (Boolean) -> Unit,
    showJigsaws: Boolean,
    onShowJigsawsChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(StudioColors.Zinc925, RoundedCornerShape(8.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(152.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Generation", style = StudioTypography.semiBold(13), color = StudioColors.Zinc100)
            ToggleLine("Animations", animations, onAnimationsChange)
            ToggleLine("Jigsaws", showJigsaws, onShowJigsawsChange)
        }
        Button(onClick = { onStepChange(selectedStep - 1) }, variant = ButtonVariant.GHOST_BORDER, size = ButtonSize.SM, text = "Prev", enabled = selectedStep > 0)
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onStepChange(selectedStep + 1) }, variant = ButtonVariant.GHOST_BORDER, size = ButtonSize.SM, text = "Next", enabled = selectedStep < template.jigsaws().size)
        Spacer(Modifier.width(12.dp))
        LazyColumn(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
            item {
                TimelineLine(0, selectedStep == 0, "Base", "${template.voxels().size} voxels visibles") { onStepChange(0) }
            }
            items(template.jigsaws().take(32).mapIndexed { index, node -> index + 1 to node }) { (index, node) ->
                TimelineLine(index, selectedStep == index, node.name().ifBlank { "jigsaw $index" }, node.target()) { onStepChange(index) }
            }
        }
    }
}

@Composable
private fun ToggleLine(text: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleSwitch(value, onChange)
        Text(text, style = StudioTypography.regular(11), color = StudioColors.Zinc400)
    }
}

@Composable
private fun TimelineLine(index: Int, active: Boolean, title: String, detail: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(if (active) StudioColors.Zinc800 else Color.Transparent, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onClick, variant = ButtonVariant.TRANSPARENT, size = ButtonSize.NONE, text = index.toString(), modifier = Modifier.width(34.dp).height(22.dp))
        Text(title, style = StudioTypography.medium(11), color = if (active) StudioColors.Zinc100 else StudioColors.Zinc300, modifier = Modifier.weight(1f))
        Text(detail, style = StudioTypography.regular(10), color = StudioColors.Zinc500)
    }
}

@Composable
private fun StructureInspector(
    context: StudioContext,
    template: StructureTemplateSnapshot,
    query: String,
    onQueryChange: (String) -> Unit,
    fromBlock: String,
    onFromBlockChange: (String) -> Unit,
    toBlock: String,
    onToBlockChange: (String) -> Unit
) {
    val filteredCounts = remember(template, query) {
        template.blockCounts().filter { query.isBlank() || it.blockId().toString().contains(query, ignoreCase = true) }
    }
    val selectedPack = context.packSelectionMemory().selectedPack()
    val canReplace = selectedPack?.writable() == true &&
        Identifier.tryParse(fromBlock) != null &&
        Identifier.tryParse(toBlock) != null &&
        fromBlock != toBlock

    Column(
        modifier = Modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(StudioColors.Zinc925, RoundedCornerShape(8.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Blocs", style = StudioTypography.semiBold(18), color = StudioColors.Zinc100)
        InputText(query, onQueryChange, placeholder = "Filtrer les blocs", focusExpand = false)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(filteredCounts, key = { it.blockId().toString() }) { count ->
                BlockCountRow(count, onPick = { onFromBlockChange(count.blockId().toString()) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Remplacement", style = StudioTypography.semiBold(13), color = StudioColors.Zinc100)
            InputText(fromBlock, onFromBlockChange, placeholder = "minecraft:stone", showSearchIcon = false, focusExpand = false)
            InputText(toBlock, onToBlockChange, placeholder = "minecraft:deepslate", showSearchIcon = false, focusExpand = false)
            Button(
                onClick = {
                    val from = Identifier.tryParse(fromBlock) ?: return@Button
                    val to = Identifier.tryParse(toBlock) ?: return@Button
                    val packId = selectedPack?.packId() ?: return@Button
                    ClientPayloadSender.send(StructureReplaceBlocksPayload(packId, template.id(), from, to))
                },
                text = if (selectedPack == null) "Pack requis" else "Remplacer",
                enabled = canReplace,
                variant = ButtonVariant.DEFAULT,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BlockCountRow(count: StructureBlockCount, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(StudioColors.Zinc900.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(StudioColors.Zinc950, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            ItemSprite(count.blockId(), 18.dp)
        }
        Spacer(Modifier.width(8.dp))
        Text(count.blockId().toString(), style = StudioTypography.regular(11), color = StudioColors.Zinc200, modifier = Modifier.weight(1f))
        Button(onClick = onPick, variant = ButtonVariant.LINK, size = ButtonSize.NONE, text = count.count().toString(), modifier = Modifier.width(54.dp).height(24.dp))
    }
}

private fun distance(voxel: StructureBlockVoxel, jigsaw: StructureJigsawNode): Int =
    abs(voxel.x() - jigsaw.x()) + abs(voxel.y() - jigsaw.y()) + abs(voxel.z() - jigsaw.z())
