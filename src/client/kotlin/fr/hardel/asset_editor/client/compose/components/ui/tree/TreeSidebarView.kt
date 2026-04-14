package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")
private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")
private val ROW_SHAPE = RoundedCornerShape(8.dp)
private val COUNT_SHAPE = RoundedCornerShape(4.dp)
private val ACTION_ICON_BOX_SIZE = 20.dp
private val ACTION_ICON_SIZE = 16.dp

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
            active = !treeState.showAll,
            onClick = treeState.onSelectChanges
        )

        SidebarActionRow(
            icon = SEARCH_ICON,
            label = I18n.get("generic:all"),
            count = treeState.totalCount,
            colorKey = "all",
            active = treeState.showAll,
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
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (active) {
            val accentColor = ColorUtils.accentColor(colorKey)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 8.dp)
                    .width(4.dp)
                    .clip(RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp))
                    .background(accentColor)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)
                .clip(ROW_SHAPE)
                .background(
                    when {
                        active -> StudioColors.Zinc800.copy(alpha = 0.8f)
                        hovered -> StudioColors.Zinc900.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                )
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(ACTION_ICON_BOX_SIZE)
                ) {
                    SvgIcon(
                        location = icon,
                        size = ACTION_ICON_SIZE,
                        tint = Color.White,
                        modifier = Modifier.alpha(0.6f)
                    )
                }

                Text(
                    text = label,
                    style = StudioTypography.medium(14),
                    color = if (active) Color.White else StudioColors.Zinc400,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = count.toString(),
                style = StudioTypography.regular(10),
                color = StudioColors.Zinc600,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(COUNT_SHAPE)
                    .background(StudioColors.Zinc900.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800,
                        COUNT_SHAPE
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
