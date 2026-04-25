package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

@Composable
fun OverlayIconButton(
    icon: Identifier,
    iconSize: Int,
    enabled: Boolean = true,
    boxSize: Int = 22,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint = when {
        !enabled -> StudioColors.Zinc700
        hovered -> StudioColors.Zinc100
        else -> StudioColors.Zinc300
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(boxSize.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (hovered && enabled) StudioColors.Zinc800.copy(alpha = 0.5f) else Color.Transparent)
            .hoverable(interaction, enabled = enabled)
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .then(
                if (enabled) Modifier.clickable(interactionSource = interaction, indication = null) { onClick() }
                else Modifier
            )
    ) {
        SvgIcon(icon, iconSize.dp, tint)
    }
}
