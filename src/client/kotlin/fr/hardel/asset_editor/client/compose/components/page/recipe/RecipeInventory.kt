package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

private const val BATCH_SIZE = 60

@Composable
fun RecipeInventory(
    context: StudioContext,
    search: String,
    onSearchChange: (String) -> Unit,
    selectedItemId: String?,
    onSelectItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allItems = remember(context.sessionMemory().worldSessionKey()) {
        BuiltInRegistries.ITEM.keySet()
            .filterNot { it.namespace == Identifier.DEFAULT_NAMESPACE && it.path == "air" }
            .sorted()
    }
    val filteredItems = remember(allItems, search) {
        val query = search.trim().lowercase()
        if (query.isBlank()) allItems
        else allItems.filter { it.toString().lowercase().contains(query) }
    }
    val selectedIdentifier = remember(selectedItemId) { selectedItemId?.let(Identifier::tryParse) }

    var visibleCount by remember { mutableIntStateOf(BATCH_SIZE) }
    var atlasVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val subscription = ItemAtlasGenerator.subscribe(Runnable { atlasVersion++ })
        onDispose(subscription::run)
    }

    LaunchedEffect(search) { visibleCount = BATCH_SIZE }

    val visibleItems by remember(filteredItems, visibleCount) {
        derivedStateOf { filteredItems.take(visibleCount) }
    }
    val hasMore = visibleCount < filteredItems.size
    val atlasReady = remember(atlasVersion) { ItemAtlasGenerator.getAtlasImage() != null }

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 10 && hasMore) {
                visibleCount = (visibleCount + BATCH_SIZE).coerceAtMost(filteredItems.size)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .border(1.dp, StudioColors.Zinc900, RoundedCornerShape(12.dp))
    ) {
        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.12f)

        Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = I18n.get("recipe:inventory.title"),
                            style = StudioTypography.bold(20),
                            color = Color.White
                        )
                        Text(
                            text = I18n.get("recipe:inventory.description"),
                            style = StudioTypography.regular(14),
                            color = StudioColors.Zinc400
                        )
                    }

                    InputText(
                        value = search,
                        onValueChange = onSearchChange,
                        placeholder = I18n.get("recipe:inventory.search"),
                        maxWidth = 256.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(StudioColors.Zinc800.copy(alpha = 0.5f))
                        .padding(vertical = 0.5.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
            ) {
                when {
                    filteredItems.isEmpty() -> InventoryStatus(I18n.get("recipe:inventory.empty"))
                    else -> LazyVerticalGrid(
                        columns = GridCells.FixedSize(56.dp),
                        state = gridState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleItems, key = { it.toString() }) { itemId ->
                            ItemsSelector.ItemCell(
                                itemId = itemId,
                                selected = itemId == selectedIdentifier,
                                onSelect = { onSelectItem(itemId.toString()) }
                            )
                        }

                        if (hasMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    Text(
                                        text = I18n.get("recipe:inventory.loading_more"),
                                        style = StudioTypography.regular(12),
                                        color = StudioColors.Zinc500
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom fade gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                if (!atlasReady && filteredItems.isNotEmpty()) {
                    InventoryLoading(
                        text = I18n.get("debug:render.loading"),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryStatus(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth()
    ) {
        Text(text = text, style = StudioTypography.regular(14), color = StudioColors.Zinc400)
    }
}

@Composable
private fun InventoryLoading(text: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(text = text, style = StudioTypography.regular(12), color = StudioColors.Zinc400)
    }
}
