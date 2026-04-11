package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

private val GRAB_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/grab.svg")

@Composable
fun ToolGrab() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(start = 16.dp)
    ) {
        SvgIcon(GRAB_ICON, 16.dp, StudioColors.Zinc500)
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(StudioColors.Zinc800.copy(alpha = 0.5f))
        )
    }
}
