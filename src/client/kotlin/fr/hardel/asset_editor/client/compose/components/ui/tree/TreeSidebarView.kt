package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")
private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")

@Composable
fun TreeSidebarView(
    treeState: ConceptTreeState,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(top = 16.dp)
    ) {
        SidebarActionRow(
            icon = PENCIL_ICON,
            label = I18n.get("generic:updated"),
            count = treeState.modifiedCount,
            colorKey = "updated",
            active = false,
            onClick = treeState.onNavigateChanges
        )

        SidebarActionRow(
            icon = SEARCH_ICON,
            label = I18n.get("generic:all"),
            count = treeState.rootCount,
            colorKey = "all",
            active = treeState.isAllActive(),
            onClick = treeState.onSelectAll
        )

        FileTreeView(treeState = treeState)
    }
}

@Composable
private fun SidebarActionRow(
    icon: Identifier,
    label: String,
    count: Int,
    colorKey: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .clip(RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp))
                    .background(ColorUtils.accentColor(colorKey))
            )
        } else {
            Box(modifier = Modifier.width(4.dp))
        }

        SvgIcon(
            location = icon,
            size = 20.dp,
            tint = Color.White,
            modifier = Modifier.alpha(0.6f)
        )

        Text(
            text = label,
            style = VoxelTypography.regular(13),
            color = if (active) Color.White else VoxelColors.Zinc400,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = count.toString(),
            style = VoxelTypography.regular(11),
            color = VoxelColors.Zinc600
        )
    }
}
