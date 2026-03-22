package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import fr.hardel.asset_editor.client.compose.lib.utils.IconUtils
import net.minecraft.resources.Identifier

private val DEFAULT_FOLDER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")
private val TREE_ROW_SHAPE = RoundedCornerShape(8.dp)
private val TREE_COUNT_SHAPE = RoundedCornerShape(4.dp)

@Composable
fun FileTreeView(
    tree: TreeController,
    modifier: Modifier = Modifier
) {
    tree.refreshState()
    val root = tree.tree ?: return
    val openState = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier) {
        for ((name, child) in sortedEntries(root.children)) {
            TreeNode(
                tree = tree,
                name = name,
                path = name,
                node = child,
                depth = 0,
                forceOpen = false,
                openState = openState
            )
        }
    }
}

@Composable
private fun TreeNode(
    tree: TreeController,
    name: String,
    path: String,
    node: TreeNodeModel,
    depth: Int,
    forceOpen: Boolean,
    openState: MutableMap<String, Boolean>
) {
    val isElement = !node.elementId.isNullOrBlank()
    val hasChildren = node.children.isNotEmpty()
    val activeElementId = tree.currentElementId()
    val hasActiveChild = !tree.disableAutoExpand && TreeUtils.hasActiveDescendant(node, activeElementId)
    val defaultOpen = forceOpen || hasActiveChild
    val isOpen = openState.getOrPut(path) { defaultOpen }
    val filterPath = tree.filterPath()
    val isHighlighted = if (isElement) {
        node.elementId == activeElementId
    } else {
        activeElementId.isNullOrBlank() && path == filterPath
    }
    val isEmpty = !isElement && node.count == 0

    val icon = node.icon ?: if (isElement) tree.elementIcon else tree.folderIcons[name] ?: DEFAULT_FOLDER_ICON
    val isDefaultFolderIcon = node.icon == null && !isElement && tree.folderIcons[name] == null
    val labelText = node.label?.takeUnless { it.isBlank() } ?: name
    val rowInteraction = remember(path) { MutableInteractionSource() }
    val chevronInteraction = remember(path) { MutableInteractionSource() }
    val isHovered by rowInteraction.collectIsHoveredAsState()
    val chevronHovered by chevronInteraction.collectIsHoveredAsState()

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isHighlighted) {
                val accentColor = ColorUtils.accentColor(if (isElement) node.elementId else path)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(vertical = 8.dp)
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp))
                        .background(accentColor)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 8 + 8).dp)
                    .clip(TREE_ROW_SHAPE)
                    .background(
                        when {
                            isHighlighted -> VoxelColors.Zinc800.copy(alpha = 0.8f)
                            isHovered -> VoxelColors.Zinc900.copy(alpha = 0.5f)
                            else -> Color.Transparent
                        }
                    )
                    .hoverable(rowInteraction)
                    .alpha(if (isEmpty && !isHighlighted) 0.5f else 1f)
            ) {
                if (!isElement) {
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
                                if (hasChildren) {
                                    openState[path] = !openState.getOrDefault(path, false)
                                }
                            }
                            .then(
                                if (hasChildren && chevronHovered) Modifier.background(VoxelColors.Zinc700.copy(alpha = 0.5f))
                                else Modifier
                            )
                    ) {
                        TreeChevron(
                            modifier = Modifier
                                .rotate(if (isOpen) 0f else -90f)
                                .alpha(if (hasChildren) 0.6f else 0.2f)
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
                            if (isElement) {
                                tree.selectElement(node.elementId!!)
                            } else {
                                openState[path] = !openState.getOrDefault(path, false)
                                tree.selectFolder(path)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(modifier = Modifier.alpha(if (isDefaultFolderIcon && !isHighlighted) 0.6f else 1f)) {
                        if (IconUtils.isSvgIcon(icon)) {
                            SvgIcon(location = icon, size = 20.dp, tint = Color.White)
                        } else {
                            ResourceImageIcon(location = icon, size = 20.dp)
                        }
                    }

                    Text(
                        text = labelText,
                        style = VoxelTypography.medium(13),
                        color = if (isHighlighted) Color.White else VoxelColors.Zinc400,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isElement) {
                    Text(
                        text = node.count.toString(),
                        style = VoxelTypography.regular(10),
                        color = VoxelColors.Zinc600,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(TREE_COUNT_SHAPE)
                            .background(VoxelColors.Zinc900.copy(alpha = 0.5f))
                            .border(1.dp, VoxelColors.Zinc800, TREE_COUNT_SHAPE)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        if (hasChildren && isOpen) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(
                            color = VoxelColors.Zinc800.copy(alpha = 0.5f),
                            start = Offset(stroke / 2f, 0f),
                            end = Offset(stroke / 2f, size.height),
                            strokeWidth = stroke
                        )
                    }
                    .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                for ((childName, childNode) in sortedEntries(node.children)) {
                    TreeNode(
                        tree = tree,
                        name = childName,
                        path = "$path/$childName",
                        node = childNode,
                        depth = depth + 1,
                        forceOpen = node.children.size == 1,
                        openState = openState
                    )
                }
            }
        }
    }
}

private fun sortedEntries(map: Map<String, TreeNodeModel>): List<Pair<String, TreeNodeModel>> =
    map.entries
        .sortedBy { !isElement(it.value) }
        .map { it.key to it.value }

private fun isElement(node: TreeNodeModel): Boolean =
    !node.elementId.isNullOrBlank()

@Composable
private fun TreeChevron(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        val stroke = 2f
        val left = size.width * 0.22f
        val midX = size.width * 0.5f
        val right = size.width * 0.78f
        val top = size.height * 0.28f
        val bottom = size.height * 0.72f

        drawLine(
            color = Color.White,
            start = Offset(left, top),
            end = Offset(midX, bottom),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(midX, bottom),
            end = Offset(right, top),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
