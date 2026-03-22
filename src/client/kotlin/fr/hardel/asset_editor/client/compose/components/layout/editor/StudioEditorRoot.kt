package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.lib.StudioContext

private val contentShape = RoundedCornerShape(topStart = 24.dp)

@Composable
fun StudioEditorRoot(context: StudioContext, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxSize().background(VoxelColors.Sidebar)) {
        StudioPrimarySidebar(
            context = context,
            modifier = Modifier.width(64.dp).fillMaxHeight()
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            StudioEditorTabsBar(context = context)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(VoxelColors.Content, contentShape)
                    .background(VoxelColors.Content)
            ) {
                ContentOutlet(context = context)
            }
        }
    }
}
