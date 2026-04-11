package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

data class ToolbarDropdownOption(val value: String, val label: String)

@Composable
fun ToolbarDropdown(
    icon: Identifier,
    value: String,
    options: List<ToolbarDropdownOption>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.find { it.value == value }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .height(40.dp)
                .background(
                    if (hovered) StudioColors.Zinc800.copy(alpha = 0.5f) else Color.Transparent,
                    RoundedCornerShape(20.dp)
                )
                .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interaction, indication = null) { expanded = !expanded }
                .padding(horizontal = 12.dp)
        ) {
            SvgIcon(icon, 16.dp, Color.White.copy(alpha = 0.75f))
            Text(selected?.label ?: "", style = StudioTypography.medium(12), color = StudioColors.Zinc300)
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false }
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 150.dp)
                        .background(StudioColors.Zinc900, RoundedCornerShape(8.dp))
                        .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    for (option in options) {
                        Text(
                            text = option.label,
                            style = StudioTypography.medium(12),
                            color = if (option.value == value) StudioColors.Zinc100 else StudioColors.Zinc400,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { onChange(option.value); expanded = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
