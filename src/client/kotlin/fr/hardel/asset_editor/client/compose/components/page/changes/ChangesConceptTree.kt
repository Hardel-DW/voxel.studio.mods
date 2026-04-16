package fr.hardel.asset_editor.client.compose.components.page.changes

import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeUtils
import fr.hardel.asset_editor.client.compose.lib.git.GitFileStatus
import fr.hardel.asset_editor.data.concept.StudioRegistryResolver
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.Identifier

object ChangesConceptTree {

    private const val MISC_KEY = "misc"
    private val MISC_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")

    data class Result(
        val tree: TreeNodeModel,
        val folderIcons: Map<String, Identifier>
    )

    fun build(
        registries: RegistryAccess?,
        status: Map<String, GitFileStatus>
    ): Result {
        val root = TreeNodeModel()
        val folderIcons = LinkedHashMap<String, Identifier>()
        if (status.isEmpty()) {
            root.count = 0
            return Result(root, folderIcons)
        }
        if (registries == null) {
            populateMiscOnly(root, status)
            folderIcons[MISC_KEY] = MISC_ICON
            TreeUtils.recalculateCount(root)
            return Result(root, folderIcons)
        }

        val index = ChangesPathParser.buildIndex(registries)
        val miscFolder = TreeNodeModel().also { it.folder = true; it.label = MISC_KEY }
        val conceptFolders = LinkedHashMap<Identifier, TreeNodeModel>()

        for ((path, _) in status) {
            val info = ChangesPathParser.parse(path, index)
            val conceptId = info.conceptId
            if (conceptId == null) {
                addElementToFolder(miscFolder, path, info, fallbackIcon = MISC_ICON)
                continue
            }
            val folder = conceptFolders.getOrPut(conceptId) {
                val node = TreeNodeModel().also {
                    it.folder = true
                    it.label = conceptId.path
                    it.icon = StudioRegistryResolver.icon(registries, conceptId)
                }
                folderIcons[conceptId.path] = StudioRegistryResolver.icon(registries, conceptId)
                node
            }
            val registryId = info.registryId
            val elementIcon = if (registryId != null) StudioRegistryResolver.elementIcon(registryId) else null
            addElementToFolder(folder, path, info, fallbackIcon = elementIcon ?: MISC_ICON)
        }

        for ((conceptId, folder) in conceptFolders) {
            root.children[conceptId.path] = folder
        }
        if (miscFolder.children.isNotEmpty()) {
            folderIcons[MISC_KEY] = MISC_ICON
            miscFolder.icon = MISC_ICON
            root.children[MISC_KEY] = miscFolder
        }
        TreeUtils.recalculateCount(root)
        return Result(root, folderIcons)
    }

    private fun populateMiscOnly(root: TreeNodeModel, status: Map<String, GitFileStatus>) {
        val miscFolder = TreeNodeModel().also {
            it.folder = true
            it.label = MISC_KEY
            it.icon = MISC_ICON
        }
        for ((path, _) in status) {
            val fileName = path.substringAfterLast('/')
            val info = ChangesPathInfo(null, null, null, null, fileName)
            addElementToFolder(miscFolder, path, info, fallbackIcon = MISC_ICON)
        }
        root.children[MISC_KEY] = miscFolder
    }

    private fun addElementToFolder(
        folder: TreeNodeModel,
        path: String,
        info: ChangesPathInfo,
        fallbackIcon: Identifier
    ) {
        val leaf = TreeNodeModel().also {
            it.elementId = path
            it.label = info.displayLabel
            it.count = 1
            it.icon = fallbackIcon
        }
        folder.children[path] = leaf
    }
}
