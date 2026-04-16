package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.tree.FileTreeView
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.components.ui.tree.buildConceptTreeState
import fr.hardel.asset_editor.client.compose.lib.git.GitFileStatus
import kotlinx.collections.immutable.toImmutableMap
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.Identifier

private val CHANGES_CONCEPT_ID = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "changes")
private val DEFAULT_ELEMENT_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")

@Composable
fun ChangesConceptTreeView(
    registries: RegistryAccess?,
    status: Map<String, GitFileStatus>,
    selectedFile: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expansion = remember { mutableStateMapOf<String, Boolean>() }
    val built = remember(registries, status) { ChangesConceptTree.build(registries, status) }
    val tree: TreeNodeModel = built.tree
    val folderIconsMap = remember(built.folderIcons) { built.folderIcons.toImmutableMap() }

    val treeState = remember(tree, selectedFile, expansion.toMap()) {
        buildConceptTreeState(
            conceptId = CHANGES_CONCEPT_ID,
            tree = tree,
            filterPath = "",
            selectedElementId = selectedFile,
            treeExpansion = expansion.toMap().toImmutableMap(),
            elementIcon = DEFAULT_ELEMENT_ICON,
            folderIcons = folderIconsMap,
            disableAutoExpand = false,
            totalCount = tree.count,
            modifiedCount = tree.count,
            showAll = false,
            onSelectAll = {},
            onSelectChanges = {},
            onSelectFolder = {},
            onSelectElement = onSelect,
            onToggleExpanded = { path, expanded -> expansion[path] = expanded }
        )
    }

    FileTreeView(treeState = treeState, modifier = modifier)
}
