package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.ItemCell
import fr.hardel.asset_editor.client.compose.components.page.debug.SpriteCell
import fr.hardel.asset_editor.client.compose.components.ui.Dropdown
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import fr.hardel.asset_editor.client.compose.lib.NativeAtlasBridge
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

private val STUDIO_ITEMS_ID = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_items")

private data class AtlasOption(val id: Identifier, val label: String)

@Composable
fun DebugRenderPage() {
    val atlasOptions = remember { buildAtlasOptions() }
    var selectedOption by remember { mutableStateOf(atlasOptions.first()) }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }

    val isStudioItems = selectedOption.id == STUDIO_ITEMS_ID

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

    val snapshot = remember(version) { NativeAtlasBridge.getSnapshot() }
    val atlasImage = remember(version) { NativeAtlasBridge.getImage() }
    val snapshotReady = snapshot != null && atlasImage != null && snapshot.atlasId() == selectedOption.id

    val filteredSprites = remember(snapshot, query) {
        if (snapshot == null) emptyList()
        else {
            val lower = query.trim().lowercase()
            val all = snapshot.sprites().entries.sortedBy { it.key.toString() }
            if (lower.isBlank()) all else all.filter { it.key.toString().contains(lower) }
        }
    }

    val loading = if (isStudioItems) ItemAtlasGenerator.getAtlasImage() == null else !snapshotReady

    val title = when {
        isStudioItems -> I18n.get("debug:render.title", filteredItems.size)
        snapshotReady -> I18n.get("debug:render.atlas.title", selectedOption.label, filteredSprites.size)
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Dropdown(
                    items = atlasOptions,
                    selected = selectedOption,
                    labelExtractor = AtlasOption::label,
                    onSelect = { option ->
                        selectedOption = option
                        query = ""
                    }
                )
                InputText(
                    value = query,
                    onValueChange = { value -> query = value },
                    placeholder = I18n.get("debug:render.search"),
                    maxWidth = 400.dp
                )
            }

            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = StudioTypography.semiBold(18),
                    color = StudioColors.Zinc100
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isStudioItems) {
                    filteredItems.forEach { itemId -> ItemCell(itemId) }
                } else if (snapshotReady) {
                    filteredSprites.forEach { (_, sprite) -> SpriteCell(atlasImage!!, sprite) }
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

private fun buildAtlasOptions(): List<AtlasOption> {
    val options = mutableListOf(
        AtlasOption(id = STUDIO_ITEMS_ID, label = I18n.get("debug:render.atlas.studio_items"))
    )

    Minecraft.getInstance().atlasManager.forEach { atlasId, _ ->
        options.add(AtlasOption(id = atlasId, label = StudioTranslation.resolve("debug:render.atlas", atlasId)))
    }

    return options
}
