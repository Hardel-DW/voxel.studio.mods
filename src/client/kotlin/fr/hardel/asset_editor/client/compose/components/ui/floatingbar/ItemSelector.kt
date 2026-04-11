package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import java.util.Locale

private const val ITEMS_PER_PAGE = 150

// TSX: grid grid-rows-[auto_1fr_auto] h-full overflow-hidden
// Header (pb-4 border-b), Body (overflow-y-auto py-4), Footer (pt-4 border-t)
@Composable
fun ItemSelector(
    currentItem: String = "",
    onItemSelect: (Identifier) -> Unit,
    onCancel: () -> Unit,
    items: List<Identifier>? = null
) {
    var search by remember { mutableStateOf("") }
    var visibleCount by remember { mutableIntStateOf(ITEMS_PER_PAGE) }
    val allItems = remember(items) {
        items ?: BuiltInRegistries.ITEM.keySet()
            .filter { it.path != "air" }
            .sortedBy { it.toString() }
    }
    val filtered = remember(allItems, search) {
        visibleCount = ITEMS_PER_PAGE
        val query = search.trim().lowercase(Locale.ROOT)
        if (query.isBlank()) allItems
        else allItems.filter { it.toString().lowercase(Locale.ROOT).contains(query) }
    }
    val visible = filtered.take(visibleCount)
    val hasMore = visibleCount < filtered.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Header — pb-4 border-b border-zinc-800/50
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            InputText(
                value = search,
                onValueChange = { search = it },
                placeholder = I18n.get("item_selector.search")
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioColors.Zinc800.copy(alpha = 0.5f)))

        // Body — overflow-y-auto py-4 min-h-0, grid gap-2
        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp), // grid-cols-items: repeat(auto-fill, minmax(52px, 1fr))
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp) // py-4
        ) {
            items(items = visible, key = { it.toString() }) { itemId ->
                ItemCell(itemId, currentItem == itemId.toString()) {
                    onItemSelect(itemId)
                }
            }

            // Load more trigger
            if (hasMore) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clickable { visibleCount += ITEMS_PER_PAGE }
                    ) {
                        Text("...", style = StudioTypography.medium(14), color = StudioColors.Zinc500)
                    }
                }
            }
        }

        // Footer — pt-4 border-t border-zinc-800/50
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioColors.Zinc800.copy(alpha = 0.5f)))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp) // pt-4
        ) {
            Text(
                text = I18n.get("item_selector.select"),
                style = StudioTypography.medium(12), // text-xs font-medium
                color = StudioColors.Zinc400 // text-zinc-400
            )
            Button(
                text = I18n.get("item_selector.cancel"),
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                onClick = onCancel
            )
        }
    }
}

// TSX: size-14 (56px), border-2 rounded (8px), cursor-pointer, transition-colors
@Composable
private fun ItemCell(itemId: Identifier, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val borderColor = when {
        selected -> StudioColors.Zinc600 // border-zinc-600
        hovered -> StudioColors.Zinc600 // hover:border-zinc-600
        else -> StudioColors.Zinc800 // border-zinc-800
    }
    val bgColor = if (selected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color.Transparent // bg-white/5 when selected

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp) // size-14
            .clip(RoundedCornerShape(8.dp)) // rounded
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)) // border-2
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        ItemSprite(itemId, 40.dp)
    }
}
