package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.MinecraftTooltipArea
import net.minecraft.resources.Identifier

object ItemsSelector {

    @Composable
    fun ItemCell(
        itemId: Identifier,
        selected: Boolean,
        onSelect: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val interaction = remember(itemId) { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()
        val highlighted = selected || hovered

        MinecraftTooltipArea(itemId = itemId, modifier = modifier) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (highlighted) StudioColors.Zinc900.copy(alpha = 0.4f) else StudioColors.Zinc900.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (highlighted) StudioColors.Zinc600 else StudioColors.Zinc800,
                        RoundedCornerShape(8.dp)
                    )
                    .hoverable(interaction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(interactionSource = interaction, indication = null) { onSelect() }
            ) {
                ItemSprite(itemId, 32.dp)
            }
        }
    }
}
