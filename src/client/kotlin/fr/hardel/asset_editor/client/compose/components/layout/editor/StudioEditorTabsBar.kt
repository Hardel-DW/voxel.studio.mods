package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import fr.hardel.asset_editor.client.compose.components.layout.loading.WindowControls
import fr.hardel.asset_editor.client.compose.routes.StudioRouter

@Composable
fun StudioEditorTabsBar(router: StudioRouter, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(start = 16.dp)
    ) {
        // Tabs will be rendered here when tab state is wired
        Text(
            text = router.currentRoute.concept.replaceFirstChar { it.uppercase() },
            style = VoxelTypography.medium(13),
            color = VoxelColors.Zinc400
        )

        Spacer(Modifier.weight(1f))

        WindowControls()
    }
}
