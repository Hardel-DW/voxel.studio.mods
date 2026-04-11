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

@Composable
fun ItemSelector(
    currentItem: String = "",
    onItemSelect: (Identifier) -> Unit,
    onCancel: () -> Unit,
    items: List<Identifier>? = null
) {
    var search by remember { mutableStateOf("") }
    val allItems = remember(items) {
        items ?: BuiltInRegistries.ITEM.keySet()
            .filter { it.path != "air" }
            .sortedBy { it.toString() }
    }
    val filtered = remember(allItems, search) {
        val query = search.trim().lowercase(Locale.ROOT)
        if (query.isBlank()) allItems
        else allItems.filter { it.toString().lowercase(Locale.ROOT).contains(query) }
    }

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

        // Body — Compose LazyVerticalGrid handles virtualization natively
        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            items(items = filtered, key = { it.toString() }) { itemId ->
                ItemCell(itemId, currentItem == itemId.toString()) {
                    onItemSelect(itemId)
                }
            }
        }

        // Footer — pt-4 border-t border-zinc-800/50
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioColors.Zinc800.copy(alpha = 0.5f)))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text(
                text = I18n.get("item_selector.select"),
                style = StudioTypography.medium(12),
                color = StudioColors.Zinc400
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

@Composable
private fun ItemCell(itemId: Identifier, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val borderColor = when {
        selected -> StudioColors.Zinc600
        hovered -> StudioColors.Zinc600
        else -> StudioColors.Zinc800
    }
    val bgColor = if (selected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)
        else androidx.compose.ui.graphics.Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
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
