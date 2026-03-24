package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import net.minecraft.resources.Identifier

private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")

private val TREE_ROW_SHAPE = RoundedCornerShape(8.dp)
private val TREE_COUNT_SHAPE = RoundedCornerShape(4.dp)

@Composable
fun FileTreeView(
    treeState: ConceptTreeState,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = treeState.rows, key = { row -> row.key }) { row ->
            TreeRow(treeState = treeState, row = row)
        }
    }
}

@Composable
private fun TreeRow(
    treeState: ConceptTreeState,
    row: TreeRowState
) {
    val rowInteraction = remember(row.key) { MutableInteractionSource() }
    val chevronInteraction = remember(row.key) { MutableInteractionSource() }
    val isHovered by rowInteraction.collectIsHoveredAsState()
    val chevronHovered by chevronInteraction.collectIsHoveredAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (row.isHighlighted) {
            val accentColor = ColorUtils.accentColor(row.elementId ?: row.path)
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
                .padding(start = (row.depth * 8 + 8).dp)
                .clip(TREE_ROW_SHAPE)
                .background(
                    when {
                        row.isHighlighted -> VoxelColors.Zinc800.copy(alpha = 0.8f)
                        isHovered -> VoxelColors.Zinc900.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                )
                .hoverable(rowInteraction)
                .alpha(if (row.isEmpty && !row.isHighlighted) 0.5f else 1f)
        ) {
            if (!row.isElement) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clip(RoundedCornerShape(6.dp))
                        .hoverable(chevronInteraction)
                        .clickable(
                            interactionSource = chevronInteraction,
                            indication = null
                        ) {
                            if (row.isExpandable) {
                                treeState.onToggleExpanded(row.path, !row.isExpanded)
                            }
                        }
                        .then(
                            if (row.isExpandable && chevronHovered) Modifier.background(VoxelColors.Zinc700.copy(alpha = 0.5f))
                            else Modifier
                        )
                ) {
                    SvgIcon(
                        location = CHEVRON_ICON,
                        size = 12.dp,
                        tint = Color.White,
                        modifier = Modifier
                            .rotate(if (row.isExpanded) 0f else -90f)
                            .alpha(if (row.isExpandable) 0.6f else 0.2f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = rowInteraction,
                        indication = null
                    ) {
                        if (row.isElement) {
                            row.elementId?.let(treeState.onSelectElement)
                        } else {
                            if (row.isExpandable) {
                                treeState.onToggleExpanded(row.path, !row.isExpanded)
                            }
                            treeState.onSelectFolder(row.path)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconCell(row)

                Text(
                    text = row.label,
                    style = VoxelTypography.medium(13),
                    color = if (row.isHighlighted) Color.White else VoxelColors.Zinc400,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            if (row.count != null) {
                Text(
                    text = row.count.toString(),
                    style = VoxelTypography.regular(10),
                    color = VoxelColors.Zinc600,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(TREE_COUNT_SHAPE)
                        .background(VoxelColors.Zinc900.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            if (isHovered) VoxelColors.Zinc700 else VoxelColors.Zinc800,
                            TREE_COUNT_SHAPE
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun IconCell(row: TreeRowState) {
    Box {
        if (row.icon.path.endsWith(".svg")) {
            SvgIcon(location = row.icon, size = 20.dp, tint = Color.White)
        } else {
            ResourceImageIcon(location = row.icon, size = 20.dp)
        }
    }
}

