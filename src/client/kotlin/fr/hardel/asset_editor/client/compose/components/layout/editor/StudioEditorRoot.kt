package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.routes.StudioRouter

private val contentShape = RoundedCornerShape(topStart = 24.dp)

@Composable
fun StudioEditorRoot(router: StudioRouter, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxSize().background(VoxelColors.Sidebar)) {
        StudioPrimarySidebar(
            router = router,
            modifier = Modifier.width(64.dp).fillMaxHeight()
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            StudioEditorTabsBar(router = router)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(contentShape)
                    .border(1.dp, VoxelColors.Zinc900, contentShape)
                    .background(VoxelColors.Content)
            ) {
                ContentOutlet(router = router)
            }
        }
    }
}
