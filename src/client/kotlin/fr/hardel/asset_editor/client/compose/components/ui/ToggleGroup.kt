package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import net.minecraft.resources.Identifier
import java.util.Locale

sealed interface ToggleOption {
    val value: String

    data class TextOption(override val value: String, val label: String) : ToggleOption
    data class IconOption(override val value: String, val iconPath: Identifier) : ToggleOption
}

@Composable
fun ToggleGroup(
    options: List<ToggleOption>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupShape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .clip(groupShape)
            .background(VoxelColors.Zinc950)
            .border(1.dp, VoxelColors.Zinc800, groupShape)
            .padding(4.dp)
    ) {
        for (option in options) {
            val isActive = option.value == selectedValue
            val interactionSource = remember(option.value) { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val optionShape = RoundedCornerShape(6.dp)
            val optionModifier = when (option) {
                is ToggleOption.TextOption -> Modifier.weight(1f)
                is ToggleOption.IconOption -> Modifier.size(28.dp)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = optionModifier
                    .then(if (isActive) Modifier.shadow(1.dp, optionShape) else Modifier)
                    .clip(optionShape)
                    .then(if (isActive) Modifier.background(VoxelColors.Zinc800) else Modifier)
                    .hoverable(interactionSource)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onValueChange(option.value) }
                    .padding(
                        horizontal = if (option is ToggleOption.TextOption) 8.dp else 6.dp,
                        vertical = 6.dp
                    )
            ) {
                if (isActive) {
                    ShineOverlay(
                        modifier = Modifier.matchParentSize(),
                        opacity = 0.12f
                    )
                }

                when (option) {
                    is ToggleOption.TextOption -> Text(
                        text = option.label.uppercase(Locale.ROOT),
                        style = VoxelTypography.bold(10),
                        color = when {
                            isActive -> VoxelColors.Zinc100
                            isHovered -> VoxelColors.Zinc300
                            else -> VoxelColors.Zinc500
                        }
                    )
                    is ToggleOption.IconOption -> SvgIcon(
                        location = option.iconPath,
                        size = 16.dp,
                        tint = Color.White,
                        modifier = Modifier.alpha(if (isActive) 1f else 0.65f)
                    )
                }
            }
        }
    }
}
