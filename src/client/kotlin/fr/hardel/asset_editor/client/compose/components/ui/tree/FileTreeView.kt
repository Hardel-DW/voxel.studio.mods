package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import fr.hardel.asset_editor.client.compose.StudioMotion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val DEFAULT_FOLDER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")

private val TREE_ROW_SHAPE = RoundedCornerShape(8.dp)
private val TREE_COUNT_SHAPE = RoundedCornerShape(4.dp)

private val DEPTH_INDENT: Dp = 16.dp
private val ROW_PADDING_START: Dp = 8.dp
private val CHEVRON_BOX_SIZE: Dp = 20.dp
private val CONTENT_GAP: Dp = 8.dp
private val GUIDE_LINE_COLOR = StudioColors.Zinc800.copy(alpha = 0.6f)

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

    val chevronRotation by animateFloatAsState(
        targetValue = if (row.isExpanded) 0f else -90f,
        animationSpec = StudioMotion.hoverSpec(),
        label = "tree-chevron"
    )

    val isDefaultFolderIcon = !row.isElement && row.icon == DEFAULT_FOLDER_ICON
    val iconAlpha = when {
        isDefaultFolderIcon && row.isHighlighted -> 1f
        isDefaultFolderIcon -> 0.6f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (row.depth == 0) return@drawBehind
                val indentPx = DEPTH_INDENT.toPx()
                val rowPaddingPx = ROW_PADDING_START.toPx()
                val chevronHalfPx = CHEVRON_BOX_SIZE.toPx() / 2f
                val strokeWidth = 1.dp.toPx()
                for (level in 0 until row.depth) {
                    val x = level * indentPx + rowPaddingPx + chevronHalfPx
                    drawLine(
                        color = GUIDE_LINE_COLOR,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .padding(start = DEPTH_INDENT * row.depth)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TREE_ROW_SHAPE)
                .background(
                    when {
                        row.isHighlighted -> StudioColors.Zinc800.copy(alpha = 0.8f)
                        isHovered -> StudioColors.Zinc900.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                )
                .hoverable(rowInteraction)
                .alpha(if (row.isEmpty && !row.isHighlighted) 0.5f else 1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CONTENT_GAP),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ROW_PADDING_START)
            ) {
                if (row.isElement) {
                    Spacer(modifier = Modifier.size(CHEVRON_BOX_SIZE))
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(CHEVRON_BOX_SIZE)
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
                                if (row.isExpandable && chevronHovered) Modifier.background(StudioColors.Zinc700.copy(alpha = 0.5f))
                                else Modifier
                            )
                    ) {
                        SvgIcon(
                            location = CHEVRON_ICON,
                            size = 12.dp,
                            tint = StudioColors.Zinc400,
                            modifier = Modifier
                                .rotate(chevronRotation)
                                .alpha(if (row.isExpandable) 1f else 0.3f)
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
                        .padding(vertical = 8.dp)
                ) {
                    IconCell(row, iconAlpha)

                    Text(
                        text = row.label,
                        style = StudioTypography.medium(14),
                        color = if (row.isHighlighted) Color.White else StudioColors.Zinc400,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (row.count != null) {
                    Text(
                        text = row.count.toString(),
                        style = StudioTypography.regular(10),
                        color = StudioColors.Zinc600,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(TREE_COUNT_SHAPE)
                            .background(StudioColors.Zinc900.copy(alpha = 0.5f))
                            .border(
                                1.dp,
                                if (isHovered) StudioColors.Zinc700 else StudioColors.Zinc800,
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
}

@Composable
private fun IconCell(row: TreeRowState, alpha: Float) {
    Box(modifier = Modifier.alpha(alpha)) {
        if (row.icon.path.endsWith(".svg")) {
            SvgIcon(location = row.icon, size = 20.dp, tint = Color.White)
        } else {
            ResourceImageIcon(location = row.icon, size = 20.dp)
        }
    }
}
