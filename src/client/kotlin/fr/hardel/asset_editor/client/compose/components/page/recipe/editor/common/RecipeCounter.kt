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
import fr.hardel.asset_editor.client.compose.components.ui.Counter
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeCounter(
    title: String,
    description: String,
    value: Int,
    max: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    min: Int = 1,
    step: Int = 1
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
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

        Counter(
            value = value,
            onValueChange = onValueChange,
            min = min,
            max = max,
            step = step,
            enabled = enabled
        )
    }
}