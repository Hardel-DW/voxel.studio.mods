package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.AtlasSpriteTile
import fr.hardel.asset_editor.client.compose.components.page.debug.STUDIO_ITEMS_ATLAS_ID
import fr.hardel.asset_editor.client.compose.components.page.debug.SpriteCell
import fr.hardel.asset_editor.client.compose.components.page.debug.buildAtlasOptions
import fr.hardel.asset_editor.client.compose.components.page.debug.toAtlasSpriteTile
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuItem
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuLabel
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuSelectTrigger
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import fr.hardel.asset_editor.client.compose.lib.NativeAtlasBridge
import fr.hardel.asset_editor.client.rendering.NativeAtlasSnapshotService
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries

@Composable
fun DebugRenderPage() {
    val atlasOptions = remember { buildAtlasOptions() }
    var selectedOption by remember { mutableStateOf(atlasOptions.first()) }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }

    val isStudioItems = selectedOption.id == STUDIO_ITEMS_ATLAS_ID

    DisposableEffect(isStudioItems) {
        val subscription = if (isStudioItems) {
            ItemAtlasGenerator.subscribe(Runnable { version++ })
        } else {
            NativeAtlasBridge.subscribe(Runnable { version++ })
        }
        onDispose(subscription::run)
    }

    DisposableEffect(selectedOption) {
        if (!isStudioItems) NativeAtlasBridge.request(selectedOption.id)
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
        nativeSnapshot != null && nativeAtlasImage != null && nativeSnapshot.atlasId() == selectedOption.id

    val nativeSprites = remember(nativeSnapshot, query) {
        if (nativeSnapshot == null) emptyList()
        else {
            val lower = query.trim().lowercase()
            val all = nativeSnapshot.sprites().values
                .sortedBy(NativeAtlasSnapshotService.SpriteRegion::spriteId)
                .map { it.toAtlasSpriteTile() }
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

    val title = when {
        isStudioItems -> I18n.get("debug:render.title", filteredItems.size)
        snapshotReady -> I18n.get("debug:render.atlas.title", selectedOption.label, nativeSprites.size)
        else -> ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DropdownMenu {
                        DropdownMenuSelectTrigger(label = selectedOption.label)
                        DropdownMenuContent {
                            DropdownMenuLabel(text = I18n.get("debug:render.atlas.dropdown_label"))
                            atlasOptions.forEach { option ->
                                DropdownMenuItem(onClick = {
                                    selectedOption = option
                                    query = ""
                                }) {
                                    Text(text = option.label, style = StudioTypography.regular(13))
                                }
                            }
                        }
                    }
                    InputText(
                        value = query,
                        onValueChange = { value -> query = value },
                        placeholder = I18n.get("debug:render.search"),
                        maxWidth = 320.dp
                    )
                }

                if (title.isNotEmpty()) {
                    Text(text = title, style = StudioTypography.regular(13), color = StudioColors.Zinc400)
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val rowHeight = (maxWidth / 16).coerceIn(40.dp, 110.dp)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (displayedImage != null && (isStudioItems || snapshotReady)) {
                        displayedSprites.forEach { sprite ->
                            SpriteCell(
                                atlasImage = displayedImage,
                                sourceX = sprite.sourceX,
                                sourceY = sprite.sourceY,
                                sourceWidth = sprite.sourceWidth,
                                sourceHeight = sprite.sourceHeight,
                                rowHeight = rowHeight
                            )
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
