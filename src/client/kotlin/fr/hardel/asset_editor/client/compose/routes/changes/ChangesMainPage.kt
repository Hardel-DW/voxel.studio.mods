package fr.hardel.asset_editor.client.compose.routes.changes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import net.minecraft.client.resources.language.I18n

@Composable
fun ChangesMainPage(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = I18n.get("changes:layout.title"),
            style = VoxelTypography.semiBold(24),
            color = VoxelColors.Zinc400
        )
    }
}
