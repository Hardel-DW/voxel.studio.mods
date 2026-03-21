package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VoxelColors.Zinc900.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        for (option in options) {
            val isActive = option.value == selectedValue
            val shape = RoundedCornerShape(8.dp)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .then(if (isActive) Modifier.background(VoxelColors.Zinc900) else Modifier)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onValueChange(option.value) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (option) {
                    is ToggleOption.TextOption -> Text(
                        text = option.label.uppercase(Locale.ROOT),
                        style = VoxelTypography.medium(12),
                        color = if (isActive) VoxelColors.Zinc100 else VoxelColors.Zinc500
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
