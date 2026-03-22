package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.StudioRouter
import net.minecraft.resources.Identifier

private val DEFAULT_ELEMENT_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/features/item/bundle_open.png")

class TreeController(
    private val router: StudioRouter,
    private val config: Config
) {

    var tree: TreeNodeModel? by mutableStateOf(config.tree)
    var folderIcons: Map<String, Identifier> by mutableStateOf(config.folderIcons)
    var elementIcon: Identifier by mutableStateOf(config.elementIcon ?: DEFAULT_ELEMENT_ICON)
    var disableAutoExpand: Boolean by mutableStateOf(config.disableAutoExpand)
    var filterPath: String by mutableStateOf("")
    var currentElementId: String? by mutableStateOf(null)

    val isAllActive: Boolean by derivedStateOf {
        filterPath.isBlank() && currentElementId.isNullOrBlank()
    }

    val modifiedCount: Int get() = config.modifiedCount?.invoke() ?: 0

    fun selectFolder(path: String) {
        config.onSelectFolder?.invoke(path) ?: run {
            filterPath = path
            clearSelection()
            router.navigate(config.overviewRoute)
        }
    }

    fun selectElement(elementId: String) {
        config.onSelectElement?.invoke(elementId) ?: run {
            currentElementId = elementId
            if (!isOnTabRoute()) {
                router.navigate(config.detailRoute)
            }
        }
    }

    fun selectAll() {
        filterPath = ""
        clearSelection()
        router.navigate(config.overviewRoute)
    }

    fun clearSelection() {
        currentElementId = null
    }

    fun toFileTreeConfig() = FileTreeConfig(
        folderIcons = folderIcons,
        elementIcon = elementIcon,
        filterPath = filterPath,
        currentElementId = currentElementId,
        disableAutoExpand = disableAutoExpand,
        onSelectFolder = ::selectFolder,
        onSelectElement = ::selectElement
    )

    private fun isOnTabRoute(): Boolean {
        val routes = config.tabRoutes
        return routes.isNotEmpty() && router.currentRoute in routes
    }

    data class Config(
        val overviewRoute: StudioRoute,
        val detailRoute: StudioRoute,
        val changesRoute: StudioRoute,
        val concept: String,
        val tabRoutes: List<StudioRoute> = emptyList(),
        val tree: TreeNodeModel? = null,
        val elementIcon: Identifier? = null,
        val folderIcons: Map<String, Identifier> = emptyMap(),
        val disableAutoExpand: Boolean = false,
        val onSelectElement: ((String) -> Unit)? = null,
        val onSelectFolder: ((String) -> Unit)? = null,
        val modifiedCount: (() -> Int)? = null
    )
}
