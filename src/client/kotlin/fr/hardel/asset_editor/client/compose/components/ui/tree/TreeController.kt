package fr.hardel.asset_editor.client.compose.components.ui.tree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.StudioRouter
import net.minecraft.resources.Identifier

private val DEFAULT_ELEMENT_ICON = Identifier.fromNamespaceAndPath(
    AssetEditor.MOD_ID,
    "textures/features/item/bundle_open.png"
)

class TreeController(
    private val router: StudioRouter,
    private val config: Config
) {

    private var localFilterPath by mutableStateOf("")
    private var localCurrentElementId by mutableStateOf<String?>(null)

    private var treeState by mutableStateOf(config.tree)
    val tree: TreeNodeModel?
        get() = treeState

    private var folderIconsState by mutableStateOf(config.folderIcons ?: emptyMap())
    val folderIcons: Map<String, Identifier>
        get() = folderIconsState

    private var elementIconState by mutableStateOf(config.elementIcon ?: DEFAULT_ELEMENT_ICON)
    val elementIcon: Identifier
        get() = elementIconState

    private var disableAutoExpandState by mutableStateOf(config.disableAutoExpand)
    val disableAutoExpand: Boolean
        get() = disableAutoExpandState

    fun overviewRoute(): StudioRoute = config.overviewRoute

    fun changesRoute(): StudioRoute = config.changesRoute

    fun concept(): String = config.concept

    fun filterPath(): String {
        val value = config.filterPath?.invoke()
        return value ?: localFilterPath
    }

    fun currentElementId(): String? {
        val selected = config.selectedElementId?.invoke()
            ?: config.currentElementId?.invoke()
            ?: localCurrentElementId
        return selected?.takeUnless { it.isBlank() }
    }

    fun isAllActive(): Boolean = filterPath().isBlank() && currentElementId().isNullOrBlank()

    fun modifiedCount(): Int = config.modifiedCount?.invoke() ?: 0

    fun setTree(value: TreeNodeModel?) {
        treeState = value
    }

    fun setFolderIcons(value: Map<String, Identifier>?) {
        folderIconsState = value ?: emptyMap()
    }

    fun setDisableAutoExpand(value: Boolean) {
        disableAutoExpandState = value
    }

    fun refreshState() {
        syncSelectionFromActiveTab()
    }

    fun selectFolder(path: String) {
        config.onSelectFolder?.invoke(path) ?: run {
            setFilterPath(path)
            clearSelection()
            router.navigate(config.overviewRoute)
        }
    }

    fun selectElement(elementId: String) {
        config.onSelectElement?.invoke(elementId) ?: run {
            config.onOpenElement?.invoke(elementId, config.detailRoute)
            if (config.onOpenElement == null) {
                setCurrentElementId(elementId)
            }
            if (!isOnTabRoute()) {
                router.navigate(config.detailRoute)
            }
        }
    }

    fun selectAll() {
        setFilterPath("")
        clearSelection()
        router.navigate(config.overviewRoute)
    }

    fun clearSelection() {
        setCurrentElementId(null)
    }

    fun navigateChanges() {
        clearSelection()
        router.navigate(config.changesRoute)
    }

    private fun isOnTabRoute(): Boolean {
        val routes = config.tabRoutes
        return routes.isNotEmpty() && router.currentRoute in routes
    }

    private fun syncSelectionFromActiveTab() {
        if (!isOnTabRoute() || !currentElementId().isNullOrBlank()) {
            return
        }

        val activeTabElementId = config.activeTabElementId?.invoke()?.takeUnless { it.isBlank() } ?: return
        setCurrentElementId(activeTabElementId)
    }

    private fun setFilterPath(value: String) {
        config.setFilterPath?.invoke(value) ?: run {
            localFilterPath = value
        }
    }

    private fun setCurrentElementId(value: String?) {
        config.setCurrentElementId?.invoke(value) ?: run {
            localCurrentElementId = value
        }
    }

    data class Config(
        val overviewRoute: StudioRoute,
        val detailRoute: StudioRoute,
        val changesRoute: StudioRoute,
        val concept: String,
        val tabRoutes: List<StudioRoute> = emptyList(),
        val tree: TreeNodeModel? = null,
        val elementIcon: Identifier? = null,
        val folderIcons: Map<String, Identifier>? = null,
        val disableAutoExpand: Boolean = false,
        val filterPath: (() -> String?)? = null,
        val setFilterPath: ((String) -> Unit)? = null,
        val currentElementId: (() -> String?)? = null,
        val setCurrentElementId: ((String?) -> Unit)? = null,
        val selectedElementId: (() -> String?)? = null,
        val activeTabElementId: (() -> String?)? = null,
        val onOpenElement: ((String, StudioRoute) -> Unit)? = null,
        val onSelectElement: ((String) -> Unit)? = null,
        val onSelectFolder: ((String) -> Unit)? = null,
        val modifiedCount: (() -> Int)? = null
    )
}
