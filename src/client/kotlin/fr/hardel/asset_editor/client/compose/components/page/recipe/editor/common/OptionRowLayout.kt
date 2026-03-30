package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun OptionRowLayout(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    control: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = VoxelTypography.semiBold(16),
                color = VoxelColors.Zinc400
            )
            Text(
                text = description,
                style = VoxelTypography.regular(12),
                color = VoxelColors.Zinc500
            )
        }

        control()
    }
}
