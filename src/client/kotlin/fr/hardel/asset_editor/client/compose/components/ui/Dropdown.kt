package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import net.minecraft.resources.Identifier

private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val dropdownShape = RoundedCornerShape(12.dp)

@Composable
fun <T> Dropdown(
    items: List<T>,
    selected: T?,
    labelExtractor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .height(32.dp)
                .clip(dropdownShape)
                .border(2.dp, VoxelColors.Zinc900, dropdownShape)
                .padding(horizontal = 12.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded }
        ) {
            Text(
                text = if (selected != null) labelExtractor(selected) else "",
                style = VoxelTypography.medium(12),
                color = VoxelColors.Zinc300,
                modifier = Modifier.weight(1f)
            )
            SvgIcon(CHEVRON_ICON, 10.dp, VoxelColors.Zinc500)
        }

        Popover(
            expanded = expanded,
            onDismiss = { expanded = false }
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                for (item in items) {
                    DropdownOption(
                        label = labelExtractor(item),
                        isSelected = item == selected,
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Text(
        text = label,
        style = VoxelTypography.medium(12),
        color = if (isSelected) VoxelColors.Zinc100 else VoxelColors.Zinc400,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .then(if (isHovered) Modifier.background(VoxelColors.Zinc800) else Modifier)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp, horizontal = 12.dp)
    )
}
