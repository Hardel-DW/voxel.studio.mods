package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugSidebar
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugSidebarEntry
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceHeader
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import fr.hardel.asset_editor.client.compose.lib.NativeAtlasBridge
import fr.hardel.asset_editor.client.rendering.NativeAtlasSnapshotService
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

private val STUDIO_ITEMS_ATLAS_ID: Identifier =
    Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_items")
private val SIDEBAR_WIDTH = 260.dp

@Composable
fun DebugRenderPage() {
    val atlasOptions = remember { buildAtlasOptions() }
    var selectedId by remember { mutableStateOf(atlasOptions.first().id) }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }

    val selectedOption = atlasOptions.first { it.id == selectedId }
    val isStudioItems = selectedId == STUDIO_ITEMS_ATLAS_ID

    DisposableEffect(isStudioItems) {
        val subscription = if (isStudioItems) {
            ItemAtlasGenerator.subscribe(Runnable { version++ })
        } else {
            NativeAtlasBridge.subscribe(Runnable { version++ })
        }
        onDispose(subscription::run)
    }

    DisposableEffect(selectedId) {
        if (!isStudioItems) NativeAtlasBridge.request(selectedId)
        onDispose {}
    }

    val allItems = remember { BuiltInRegistries.ITEM.keySet().toList() }
    val filteredItems = remember(allItems, query) {
        val lower = query.trim().lowercase()
        if (lower.isBlank()) allItems else allItems.filter { it.toString().contains(lower) }
    }

    val nativeSnapshot = remember(version) { NativeAtlasBridge.getSnapshot() }
    val nativeAtlasImage = remember(version) { NativeAtlasBridge.getImage() }
    val studioAtlasImage = remember(version) { ItemAtlasGenerator.getAtlasImage() }
    val snapshotReady =
        nativeSnapshot != null && nativeAtlasImage != null && nativeSnapshot.atlasId() == selectedId

    val nativeSprites = remember(nativeSnapshot, query) {
        if (nativeSnapshot == null) emptyList()
        else {
            val lower = query.trim().lowercase()
            val all = nativeSnapshot.sprites().values
                .sortedBy(NativeAtlasSnapshotService.SpriteRegion::spriteId)
                .map {
                    AtlasSpriteTile(
                        id = it.spriteId(),
                        sourceX = it.sourceX(),
                        sourceY = it.sourceY(),
                        sourceWidth = it.sourceWidth(),
                        sourceHeight = it.sourceHeight()
                    )
                }
            if (lower.isBlank()) all else all.filter { it.id.toString().contains(lower) }
        }
    }

    val studioSprites = remember(version, filteredItems) {
        filteredItems.mapNotNull { itemId ->
            ItemAtlasGenerator.getEntry(itemId)?.let { entry ->
                AtlasSpriteTile(
                    id = itemId,
                    sourceX = entry.x(),
                    sourceY = entry.y(),
                    sourceWidth = entry.size(),
                    sourceHeight = entry.size()
                )
            }
        }
    }

    val displayedImage = if (isStudioItems) studioAtlasImage else nativeAtlasImage
    val displayedSprites = if (isStudioItems) studioSprites else nativeSprites
    val loading = if (isStudioItems) displayedImage == null else !snapshotReady

    val subtitle = when {
        isStudioItems -> I18n.get("debug:render.title", filteredItems.size)
        snapshotReady -> I18n.get("debug:render.atlas.title", selectedOption.label, nativeSprites.size)
        else -> ""
    }

    val entries = atlasOptions.map { option ->
        DebugSidebarEntry(
            id = option.id,
            icon = icon("eye"),
            label = option.label
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        DebugSidebar(
            entries = entries,
            selectedId = selectedId,
            onSelect = { selectedId = it; query = "" },
            sectionLabel = I18n.get("debug:render.nav.section"),
            modifier = Modifier.width(SIDEBAR_WIDTH).fillMaxHeight()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(StudioColors.Zinc950)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                DebugWorkspaceHeader(
                    title = selectedOption.label,
                    subtitle = subtitle.ifBlank { null },
                    actions = {
                        InputText(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = I18n.get("debug:render.search"),
                            maxWidth = 320.dp
                        )
                    }
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val rowHeight = (maxWidth / 16).coerceIn(40.dp, 110.dp)
                            val cellPadding = 4.dp

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (displayedImage != null && (isStudioItems || snapshotReady)) {
                                    displayedSprites.forEach { sprite ->
                                        if (sprite.sourceWidth > 0 && sprite.sourceHeight > 0) {
                                            val aspectRatio = sprite.sourceWidth.toFloat() / sprite.sourceHeight.toFloat()
                                            val displayWidth = rowHeight * aspectRatio

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .width(displayWidth + cellPadding * 2)
                                                    .height(rowHeight + cellPadding * 2)
                                                    .background(StudioColors.Zinc900, RoundedCornerShape(4.dp))
                                                    .padding(cellPadding)
                                            ) {
                                                Canvas(
                                                    modifier = Modifier
                                                        .width(displayWidth)
                                                        .height(rowHeight)
                                                ) {
                                                    drawImage(
                                                        image = displayedImage,
                                                        srcOffset = IntOffset(sprite.sourceX, sprite.sourceY),
                                                        srcSize = IntSize(sprite.sourceWidth, sprite.sourceHeight),
                                                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                                        filterQuality = FilterQuality.None
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (loading) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.matchParentSize()
                        ) {
                            Text(
                                text = I18n.get("debug:render.loading"),
                                style = StudioTypography.semiBold(18),
                                color = StudioColors.Zinc400
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class AtlasOption(val id: Identifier, val label: String)

private data class AtlasSpriteTile(
    val id: Identifier,
    val sourceX: Int,
    val sourceY: Int,
    val sourceWidth: Int,
    val sourceHeight: Int
)

private fun buildAtlasOptions(): List<AtlasOption> {
    val options = mutableListOf(
        AtlasOption(id = STUDIO_ITEMS_ATLAS_ID, label = I18n.get("debug:render.atlas.studio_items"))
    )
    Minecraft.getInstance().atlasManager.forEach { atlasId, _ ->
        options.add(AtlasOption(id = atlasId, label = StudioTranslation.resolve("debug:render.atlas", atlasId)))
    }
    return options
}

private fun icon(name: String): Identifier =
    Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/$name.svg")
