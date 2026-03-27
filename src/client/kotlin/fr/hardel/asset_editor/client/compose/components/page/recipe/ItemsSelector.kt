package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import net.minecraft.resources.Identifier

@Composable
fun ItemsSelector(
    items: List<Identifier>,
    selectedItemId: Identifier?,
    onSelectItem: (Identifier) -> Unit,
    modifier: Modifier = Modifier
) {
    // TSX: div.grid.gap-2.justify-center.auto-rows-[56px].grid-cols-[repeat(auto-fill,56px)]
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items.forEach { itemId ->
            val interaction = remember(itemId) { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val active = selectedItemId == itemId

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = when {
                            active -> VoxelColors.Zinc900.copy(alpha = 0.4f)
                            hovered -> VoxelColors.Zinc900.copy(alpha = 0.4f)
                            else -> VoxelColors.Zinc900.copy(alpha = 0.2f)
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        when {
                            active -> VoxelColors.Zinc600
                            hovered -> VoxelColors.Zinc600
                            else -> VoxelColors.Zinc800
                        },
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .hoverable(interaction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onSelectItem(itemId) }
            ) {
                ItemSprite(itemId, 32.dp)
            }
        }
    }
}
