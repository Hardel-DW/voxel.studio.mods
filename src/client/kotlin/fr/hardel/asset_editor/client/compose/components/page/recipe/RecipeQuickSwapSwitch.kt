package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.client.resources.language.I18n

private val containerShape = RoundedCornerShape(8.dp)
private val pillShape = RoundedCornerShape(6.dp)

/**
 * Small inline pill rendering [currentLabel] | [partnerLabel] where the current side is filled
 * and the partner side reacts to hover. Clicking either half (or the container) flips to the
 * partner. Only renders when a partner exists for the current serializer.
 */
@Composable
fun RecipeQuickSwapSwitch(
    currentLabel: String,
    partnerLabel: String,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800,
        animationSpec = StudioMotion.hoverSpec(),
        label = "quickswap-border"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .height(32.dp)
            .clip(containerShape)
            .border(1.dp, borderColor, containerShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.4f), containerShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onSwap
            )
            .padding(3.dp)
    ) {
        SwitchPill(label = currentLabel, active = true)
        SwitchPill(label = partnerLabel, active = false, hovered = hovered)
    }
}

@Composable
private fun SwitchPill(label: String, active: Boolean, hovered: Boolean = false) {
    val bgColor by animateColorAsState(
        targetValue = when {
            active -> StudioColors.Zinc700.copy(alpha = 0.8f)
            hovered -> StudioColors.Zinc800.copy(alpha = 0.6f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "quickswap-pill-bg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            active -> StudioColors.Zinc100
            hovered -> StudioColors.Zinc200
            else -> StudioColors.Zinc500
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "quickswap-pill-text"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(pillShape)
            .background(bgColor, pillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = I18n.get(label),
            style = StudioTypography.medium(12),
            color = textColor
        )
    }
}
