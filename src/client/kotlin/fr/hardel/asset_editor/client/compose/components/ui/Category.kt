package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography

@Composable
fun Category(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(VoxelColors.Zinc700)
            )
            Text(
                text = title,
                style = VoxelTypography.semiBold(24),
                color = VoxelColors.Zinc100,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(VoxelColors.Zinc700)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}
