package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuItem
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuTrigger
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

data class ToolbarDropdownOption(val value: String, val label: String)

private val triggerShape = RoundedCornerShape(20.dp)

@Composable
fun ToolbarDropdown(
    icon: Identifier,
    value: String,
    options: List<ToolbarDropdownOption>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = options.find { it.value == value }

    DropdownMenu {
        DropdownMenuTrigger(
            modifier = modifier
                .height(40.dp)
                .clip(triggerShape)
                .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.5f), triggerShape)
                .background(Color.Transparent, triggerShape)
                .padding(horizontal = 12.dp)
        ) {
            SvgIcon(icon, 16.dp, Color.White.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selected?.label ?: "",
                style = StudioTypography.medium(12),
                color = StudioColors.Zinc300
            )
        }

        DropdownMenuContent(minWidth = 150.dp) {
            options.forEach { option ->
                DropdownMenuItem(onClick = { onChange(option.value) }) {
                    Text(
                        text = option.label,
                        style = StudioTypography.medium(12)
                    )
                }
            }
        }
    }
}
