package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography

@Composable
fun SwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    lockText: String? = null
) {
    val descriptionText = if (locked && lockText != null) lockText else description

    SimpleCard(
        padding = PaddingValues(24.dp),
        modifier = modifier.alpha(if (locked) 0.5f else 1f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = VoxelTypography.regular(14),
                    color = VoxelColors.Zinc100
                )
                Text(
                    text = descriptionText,
                    style = VoxelTypography.light(12),
                    color = VoxelColors.Zinc400
                )
            }

            ToggleSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = !locked
            )
        }
    }
}
