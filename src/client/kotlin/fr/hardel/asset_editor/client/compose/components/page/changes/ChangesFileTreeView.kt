package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.git.GitFileStatus
import net.minecraft.resources.Identifier

private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val FOLDER = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")

private data class FileNode(
    val name: String,
    val path: String,
    val depth: Int,
    val isFile: Boolean,
    val status: GitFileStatus?,
    val childCount: Int,
    val parentPath: String?
)

@Composable
fun ChangesFileTreeView(
    status: Map<String, GitFileStatus>,
    selectedFile: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expansion = remember { mutableStateMapOf<String, Boolean>() }
    val rows = remember(status, expansion.toMap()) {
        flattenTree(status, expansion)
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = rows, key = { it.path + ":" + it.depth }) { row ->
            FileRow(
                row = row,
                isSelected = row.isFile && row.path == selectedFile,
                onClick = {
                    if (row.isFile) onSelect(row.path)
                    else expansion[row.path] = !(expansion[row.path] ?: false)
                }
            )
        }
    }
}

@Composable
private fun FileRow(
    row: FileNode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember(row.path + row.depth) { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(start = (row.depth * 12).dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .width(3.dp)
                    .height(16.dp)
                    .align(Alignment.CenterStart)
                    .background(StudioColors.Sky400, RoundedCornerShape(2.dp))
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    when {
                        isSelected -> StudioColors.Zinc800.copy(alpha = 0.85f)
                        isHovered -> StudioColors.Zinc900.copy(alpha = 0.55f)
                        else -> Color.Transparent
                    }
                )
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 8.dp)
        ) {
            if (row.isFile) {
                StatusGlyph(row.status)
            } else {
                ChevronGlyph(expanded = row.childCount > 0)
                SvgIcon(location = FOLDER, size = 14.dp, tint = StudioColors.Zinc400, modifier = Modifier.alpha(0.6f))
            }
            Text(
                text = row.name,
                style = StudioTypography.regular(11),
                color = if (isSelected) Color.White else StudioColors.Zinc400,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (!row.isFile && row.childCount > 0) {
                Text(
                    text = row.childCount.toString(),
                    style = StudioTypography.regular(10),
                    color = StudioColors.Zinc600
                )
            }
        }
    }
}

@Composable
private fun StatusGlyph(status: GitFileStatus?) {
    val (label, color) = when (status) {
        GitFileStatus.ADDED, GitFileStatus.UNTRACKED -> "A" to StudioColors.Green500
        GitFileStatus.MODIFIED -> "M" to StudioColors.Amber400
        GitFileStatus.DELETED -> "D" to StudioColors.Red500
        GitFileStatus.RENAMED -> "R" to StudioColors.Sky400
        GitFileStatus.CONFLICTED -> "!" to StudioColors.Red400
        else -> "·" to StudioColors.Zinc600
    }
    Text(
        text = label,
        style = StudioTypography.bold(10),
        color = color,
        modifier = Modifier.width(12.dp)
    )
}

@Composable
private fun ChevronGlyph(expanded: Boolean) {
    SvgIcon(
        location = CHEVRON,
        size = 10.dp,
        tint = StudioColors.Zinc500,
        modifier = Modifier.rotate(if (expanded) 0f else -90f)
    )
}

private fun flattenTree(
    status: Map<String, GitFileStatus>,
    expansion: Map<String, Boolean>
): List<FileNode> {
    val root = buildFolderTree(status)
    val rows = ArrayList<FileNode>()
    visit(rows, root, expansion, depth = 0, parentPath = null)
    return rows
}

private class FolderNode(val name: String, val fullPath: String) {
    val children = LinkedHashMap<String, FolderNode>()
    var status: GitFileStatus? = null
    var isFile: Boolean = false
}

private fun buildFolderTree(status: Map<String, GitFileStatus>): FolderNode {
    val root = FolderNode("", "")
    for ((path, fileStatus) in status) {
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) continue
        var current = root
        var accumulated = ""
        for ((index, part) in parts.withIndex()) {
            accumulated = if (accumulated.isEmpty()) part else "$accumulated/$part"
            val child = current.children.getOrPut(part) { FolderNode(part, accumulated) }
            if (index == parts.lastIndex) {
                child.isFile = true
                child.status = fileStatus
            }
            current = child
        }
    }
    return root
}

private fun visit(
    rows: MutableList<FileNode>,
    node: FolderNode,
    expansion: Map<String, Boolean>,
    depth: Int,
    parentPath: String?
) {
    val sortedChildren = node.children.values.sortedWith(
        compareBy({ it.isFile }, { it.name })
    )
    for (child in sortedChildren) {
        val expanded = expansion[child.fullPath] ?: true
        rows += FileNode(
            name = child.name,
            path = child.fullPath,
            depth = depth,
            isFile = child.isFile,
            status = child.status,
            childCount = child.children.size,
            parentPath = parentPath
        )
        if (!child.isFile && expanded) {
            visit(rows, child, expansion, depth + 1, child.fullPath)
        }
    }
}
