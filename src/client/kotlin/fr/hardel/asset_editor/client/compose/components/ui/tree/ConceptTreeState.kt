package fr.hardel.asset_editor.client.compose.components.ui.tree

import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import net.minecraft.resources.Identifier

private val DEFAULT_ELEMENT_ICON = Identifier.fromNamespaceAndPath(
    AssetEditor.MOD_ID,
    "textures/features/item/bundle_open.png"
)
private val DEFAULT_FOLDER_ICON = Identifier.fromNamespaceAndPath(
    AssetEditor.MOD_ID,
    "icons/folder.svg"
)

data class TreeRowState(
    val key: String,
    val path: String,
    val depth: Int,
    val label: String,
    val icon: Identifier,
    val count: Int?,
    val isElement: Boolean,
    val elementId: String?,
    val isExpanded: Boolean,
    val isExpandable: Boolean,
    val isHighlighted: Boolean,
    val isEmpty: Boolean
)

data class ConceptTreeState(
    val concept: StudioConcept,
    val tree: TreeNodeModel?,
    val rows: ImmutableList<TreeRowState>,
    val rootCount: Int,
    val filterPath: String,
    val selectedElementId: String?,
    val modifiedCount: Int,
    val onSelectAll: () -> Unit,
    val onSelectFolder: (String) -> Unit,
    val onSelectElement: (String) -> Unit,
    val onToggleExpanded: (String, Boolean) -> Unit,
    val onNavigateChanges: () -> Unit
) {
    fun isAllActive(): Boolean = filterPath.isBlank() && selectedElementId.isNullOrBlank()
}

fun buildConceptTreeState(
    concept: StudioConcept,
    tree: TreeNodeModel?,
    filterPath: String,
    selectedElementId: String?,
    expandedTreePaths: ImmutableSet<String>,
    elementIcon: Identifier,
    folderIcons: Map<String, Identifier>,
    disableAutoExpand: Boolean,
    modifiedCount: Int = 0,
    onSelectAll: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectElement: (String) -> Unit,
    onToggleExpanded: (String, Boolean) -> Unit,
    onNavigateChanges: () -> Unit
): ConceptTreeState {
    val rows = if (tree == null) {
        emptyList()
    } else {
        buildRows(
            root = tree,
            expandedTreePaths = expandedTreePaths,
            selectedElementId = selectedElementId,
            filterPath = filterPath,
            elementIcon = elementIcon,
            folderIcons = folderIcons,
            disableAutoExpand = disableAutoExpand
        )
    }

    return ConceptTreeState(
        concept = concept,
        tree = tree,
        rows = rows.toImmutableList(),
        rootCount = tree?.count ?: 0,
        filterPath = filterPath,
        selectedElementId = selectedElementId?.takeUnless { it.isBlank() },
        modifiedCount = modifiedCount,
        onSelectAll = onSelectAll,
        onSelectFolder = onSelectFolder,
        onSelectElement = onSelectElement,
        onToggleExpanded = onToggleExpanded,
        onNavigateChanges = onNavigateChanges
    )
}

private fun buildRows(
    root: TreeNodeModel,
    expandedTreePaths: ImmutableSet<String>,
    selectedElementId: String?,
    filterPath: String,
    elementIcon: Identifier,
    folderIcons: Map<String, Identifier>,
    disableAutoExpand: Boolean
): List<TreeRowState> {
    val rows = ArrayList<TreeRowState>()
    sortedEntries(root.children).forEach { (name, child) ->
        appendRows(
            rows = rows,
            name = name,
            path = name,
            node = child,
            depth = 0,
            forceOpen = false,
            expandedTreePaths = expandedTreePaths,
            selectedElementId = selectedElementId,
            filterPath = filterPath,
            elementIcon = elementIcon,
            folderIcons = folderIcons,
            disableAutoExpand = disableAutoExpand
        )
    }
    return rows
}

private fun appendRows(
    rows: MutableList<TreeRowState>,
    name: String,
    path: String,
    node: TreeNodeModel,
    depth: Int,
    forceOpen: Boolean,
    expandedTreePaths: ImmutableSet<String>,
    selectedElementId: String?,
    filterPath: String,
    elementIcon: Identifier,
    folderIcons: Map<String, Identifier>,
    disableAutoExpand: Boolean
) {
    val isElement = !node.elementId.isNullOrBlank()
    val hasChildren = node.children.isNotEmpty()
    val hasActiveChild = !disableAutoExpand && TreeUtils.hasActiveDescendant(node, selectedElementId)
    val defaultExpanded = forceOpen || hasActiveChild
    val isExpanded = hasChildren && (defaultExpanded || path in expandedTreePaths)
    val isHighlighted = if (isElement) {
        node.elementId == selectedElementId
    } else {
        selectedElementId.isNullOrBlank() && path == filterPath
    }
    val icon = when {
        node.icon != null -> node.icon!!
        isElement -> elementIcon
        else -> folderIcons[name] ?: DEFAULT_FOLDER_ICON
    }
    val isEmpty = !isElement && node.count == 0

    rows += TreeRowState(
        key = if (isElement) "element:$path" else "folder:$path",
        path = path,
        depth = depth,
        label = node.label?.takeUnless { it.isBlank() } ?: name,
        icon = icon,
        count = if (isElement) null else node.count,
        isElement = isElement,
        elementId = node.elementId,
        isExpanded = isExpanded,
        isExpandable = hasChildren,
        isHighlighted = isHighlighted,
        isEmpty = isEmpty
    )

    if (!isExpanded) {
        return
    }

    sortedEntries(node.children).forEach { (childName, childNode) ->
        appendRows(
            rows = rows,
            name = childName,
            path = "$path/$childName",
            node = childNode,
            depth = depth + 1,
            forceOpen = node.children.size == 1,
            expandedTreePaths = expandedTreePaths,
            selectedElementId = selectedElementId,
            filterPath = filterPath,
            elementIcon = elementIcon,
            folderIcons = folderIcons,
            disableAutoExpand = disableAutoExpand
        )
    }
}

private fun sortedEntries(map: Map<String, TreeNodeModel>): List<Pair<String, TreeNodeModel>> =
    map.entries
        .sortedBy { !isElement(it.value) }
        .map { it.key to it.value }

private fun isElement(node: TreeNodeModel): Boolean =
    !node.elementId.isNullOrBlank()
