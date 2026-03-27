package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.Pagination
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import kotlin.math.floor

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
        if (query.isBlank()) {
            allItems
        } else {
            allItems.filter { itemId -> itemId.toString().lowercase().contains(query) }
        }
    }
    val selectedIdentifier = remember(selectedItemId) { selectedItemId?.let(Identifier::tryParse) }

    var currentPage by remember(context.sessionMemory().worldSessionKey()) { mutableIntStateOf(0) }
    var atlasVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val subscription = ItemAtlasGenerator.subscribe(Runnable { atlasVersion++ })
        onDispose(subscription::run)
    }

    LaunchedEffect(search) {
        currentPage = 0
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .border(1.dp, VoxelColors.Zinc900, RoundedCornerShape(12.dp))
    ) {
        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.12f)

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            // TSX: div.px-6.pt-6.shrink-0
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = I18n.get("recipe:inventory.title"),
                            style = VoxelTypography.bold(20),
                            color = Color.White
                        )
                        Text(
                            text = I18n.get("recipe:inventory.description"),
                            style = VoxelTypography.regular(14),
                            color = VoxelColors.Zinc400
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
                        .background(VoxelColors.Zinc800.copy(alpha = 0.5f))
                        .padding(vertical = 0.5.dp)
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
            ) {
                val itemSize = 56.dp
                val gap = 8.dp
                val columns = remember(maxWidth) {
                    computeGridCount(maxWidth, itemSize, gap, 6)
                }
                val rows = remember(maxHeight) {
                    computeGridCount(maxHeight, itemSize, gap, 8)
                }
                val pageSize = (columns * rows).coerceAtLeast(1)
                val totalPages = maxOf(1, (filteredItems.size + pageSize - 1) / pageSize)

                LaunchedEffect(totalPages) {
                    currentPage = currentPage.coerceAtMost(totalPages - 1)
                }

                val pageItems = remember(filteredItems, currentPage, pageSize) {
                    val start = (currentPage * pageSize).coerceAtMost(filteredItems.size)
                    filteredItems.subList(start, minOf(start + pageSize, filteredItems.size))
                }
                val atlasReady = remember(atlasVersion) { ItemAtlasGenerator.getAtlasImage() != null }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when {
                            filteredItems.isEmpty() -> InventoryStatus(I18n.get("recipe:inventory.empty"))
                            else -> ItemsSelector(
                                items = pageItems,
                                selectedItemId = selectedIdentifier,
                                onSelectItem = { itemId -> onSelectItem(itemId.toString()) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!atlasReady && filteredItems.isNotEmpty()) {
                            InventoryLoading(
                                text = I18n.get("debug:render.loading"),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    Pagination(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageChange = { page -> currentPage = page },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
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
        Text(
            text = text,
            style = VoxelTypography.regular(14),
            color = VoxelColors.Zinc400
        )
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
        Text(
            text = text,
            style = VoxelTypography.regular(12),
            color = VoxelColors.Zinc400
        )
    }
}

private fun computeGridCount(available: Dp, cell: Dp, gap: Dp, fallback: Int): Int {
    if (available == Dp.Infinity) {
        return fallback
    }
    val count = floor(((available.value + gap.value) / (cell.value + gap.value)).toDouble()).toInt()
    return count.coerceAtLeast(1)
}
