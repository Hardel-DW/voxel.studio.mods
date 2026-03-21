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
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography

@Composable
fun Selector(
    title: String,
    description: String,
    options: LinkedHashMap<String, String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SimpleCard(
        padding = PaddingValues(vertical = 24.dp, horizontal = 48.dp),
        modifier = modifier
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
                    text = description,
                    style = VoxelTypography.light(12),
                    color = VoxelColors.Zinc400
                )
            }

            AnimatedTabs(
                options = options,
                selectedValue = selectedValue,
                onValueChange = onValueChange
            )
        }
    }
}
