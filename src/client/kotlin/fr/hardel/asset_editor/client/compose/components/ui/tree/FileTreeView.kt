package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import net.minecraft.resources.Identifier

private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val DEFAULT_FOLDER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")

data class FileTreeConfig(
    val folderIcons: Map<String, Identifier> = emptyMap(),
    val elementIcon: Identifier? = null,
    val filterPath: String? = null,
    val currentElementId: String? = null,
    val disableAutoExpand: Boolean = false,
    val onSelectFolder: (String) -> Unit = {},
    val onSelectElement: (String) -> Unit = {}
)

@Composable
fun FileTreeView(
    root: TreeNodeModel?,
    config: FileTreeConfig,
    modifier: Modifier = Modifier
) {
    if (root == null) return

    val openState = remember { mutableStateMapOf<String, Boolean>() }
    val sorted = remember(root) { sortedEntries(root.children) }

    Column(modifier = modifier) {
        for ((name, child) in sorted) {
            TreeNode(
                name = name,
                path = name,
                node = child,
                depth = 0,
                forceOpen = false,
                config = config,
                openState = openState
            )
        }
    }
}

@Composable
private fun TreeNode(
    name: String,
    path: String,
    node: TreeNodeModel,
    depth: Int,
    forceOpen: Boolean,
    config: FileTreeConfig,
    openState: MutableMap<String, Boolean>
) {
    val isElement = !node.elementId.isNullOrBlank()
    val hasChildren = node.children.isNotEmpty()
    val hasActiveChild = !config.disableAutoExpand && TreeUtils.hasActiveDescendant(node, config.currentElementId)
    val defaultOpen = forceOpen || hasActiveChild
    val isOpen = openState.getOrPut(path) { defaultOpen }

    val isHighlighted = if (isElement) {
        node.elementId == config.currentElementId
    } else {
        config.currentElementId.isNullOrBlank() && path == config.filterPath
    }
    val isEmpty = !isElement && node.count == 0

    val icon = node.icon
        ?: if (isElement) config.elementIcon
        else config.folderIcons[name] ?: DEFAULT_FOLDER_ICON
    val isDefaultFolder = node.icon == null && !isElement && config.folderIcons[name] == null

    val labelText = if (node.label.isNullOrBlank()) name else node.label!!

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            val interaction = remember { MutableInteractionSource() }
            val isHovered by interaction.collectIsHoveredAsState()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isEmpty && !isHighlighted) 0.5f else 1f)
                    .let { if (isHighlighted) it.background(VoxelColors.Zinc800.copy(alpha = 0.8f), RoundedCornerShape(6.dp)) else it }
                    .hoverable(interaction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(interactionSource = interaction, indication = null) {
                        if (isElement) {
                            config.onSelectElement(node.elementId!!)
                        } else {
                            openState[path] = !isOpen
                            config.onSelectFolder(path)
                        }
                    }
                    .padding(vertical = 4.dp)
                    .padding(start = (depth * 8 + 8).dp, end = 8.dp)
            ) {
                if (!isElement) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (isOpen) 0f else -90f)
                            .alpha(if (hasChildren) 0.6f else 0.2f)
                    ) {
                        SvgIcon(location = CHEVRON_ICON, size = 12.dp, tint = Color.White)
                    }
                }

                if (icon != null) {
                    Box(modifier = Modifier.alpha(if (isDefaultFolder && !isHighlighted) 0.6f else 1f)) {
                        SvgIcon(location = icon, size = 20.dp, tint = Color.White)
                    }
                }

                Text(
                    text = labelText,
                    style = VoxelTypography.regular(13),
                    color = if (isHighlighted) Color.White else VoxelColors.Zinc400,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (!isElement) {
                    Text(
                        text = node.count.toString(),
                        style = VoxelTypography.regular(11),
                        color = VoxelColors.Zinc600
                    )
                }
            }

            if (isHighlighted) {
                val accentColor = ColorUtils.accentColor(if (isElement) node.elementId else path)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }
        }

        if (hasChildren && isOpen) {
            val childEntries = remember(node) { sortedEntries(node.children) }
            val childForceOpen = node.children.size == 1

            Column {
                for ((childName, childNode) in childEntries) {
                    TreeNode(
                        name = childName,
                        path = "$path/$childName",
                        node = childNode,
                        depth = depth + 1,
                        forceOpen = childForceOpen,
                        config = config,
                        openState = openState
                    )
                }
            }
        }
    }
}

private fun sortedEntries(map: Map<String, TreeNodeModel>): List<Pair<String, TreeNodeModel>> =
    map.entries
        .sortedBy { !it.value.elementId.isNullOrBlank() }
        .map { it.key to it.value }
