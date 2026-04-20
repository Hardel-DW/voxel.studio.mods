package fr.hardel.asset_editor.client.compose.components.page.debug.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel.DebugWorkspaceNavigationPanel
import fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel.DebugWorkspacePacksPanel
import fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel.DebugWorkspacePendingPanel
import fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel.DebugWorkspaceRegistriesPanel
import fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel.DebugWorkspaceSessionPanel
import fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel.DebugWorkspaceUiStatePanel
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberMemoryValue
import net.minecraft.resources.Identifier

typealias DebugWorkspaceCountProvider = @Composable (StudioContext) -> Int?
typealias DebugWorkspaceRenderer = @Composable (StudioContext, Modifier) -> Unit

class DebugWorkspacePanel(
    val id: Identifier,
    val icon: Identifier,
    val labelKey: String,
    val descriptionKey: String,
    val count: DebugWorkspaceCountProvider,
    val render: DebugWorkspaceRenderer
)

/**
 * Registry of panels shown in the Debug Workspace inspector.
 *
 * Downstream mods register their own inspector by calling [register] during client init,
 * typically from an `AssetEditorClient`-style entry point:
 *
 *     DebugWorkspaceRegistry.register(
 *         DebugWorkspacePanel(
 *             id = Identifier.fromNamespaceAndPath("mymod", "economy"),
 *             icon = Identifier.fromNamespaceAndPath("mymod", "icons/coin.svg"),
 *             labelKey = "debug:workspace.nav.mymod.economy.label",
 *             descriptionKey = "debug:workspace.nav.mymod.economy.description",
 *             count = { context -> rememberMemoryValue(context.myMemory()) { it.size } },
 *             render = { context, modifier -> EconomyDebugPanel(context, modifier) }
 *         )
 *     )
 *
 * The panel id must be unique. Order of registration is preserved in the sidebar.
 */
object DebugWorkspaceRegistry {

    private val panels = linkedMapOf<Identifier, DebugWorkspacePanel>()

    init {
        register(
            DebugWorkspacePanel(
                id = builtin("session"),
                icon = builtinIcon("lock"),
                labelKey = "debug:workspace.nav.session.label",
                descriptionKey = "debug:workspace.nav.session.description",
                count = { null },
                render = { context, modifier -> DebugWorkspaceSessionPanel(context, modifier) }
            )
        )
        register(
            DebugWorkspacePanel(
                id = builtin("packs"),
                icon = builtinIcon("folder"),
                labelKey = "debug:workspace.nav.packs.label",
                descriptionKey = "debug:workspace.nav.packs.description",
                count = { context ->
                    rememberMemoryValue(context.sessionMemory()) { it.availablePacks.size }
                },
                render = { context, modifier -> DebugWorkspacePacksPanel(context, modifier) }
            )
        )
        register(
            DebugWorkspacePanel(
                id = builtin("navigation"),
                icon = builtinIcon("git-branch"),
                labelKey = "debug:workspace.nav.navigation.label",
                descriptionKey = "debug:workspace.nav.navigation.description",
                count = { context ->
                    rememberMemoryValue(context.navigationMemory()) { it.tabs.size }
                },
                render = { context, modifier -> DebugWorkspaceNavigationPanel(context, modifier) }
            )
        )
        register(
            DebugWorkspacePanel(
                id = builtin("ui_state"),
                icon = builtinIcon("pencil"),
                labelKey = "debug:workspace.nav.ui_state.label",
                descriptionKey = "debug:workspace.nav.ui_state.description",
                count = { context ->
                    rememberMemoryValue(context.uiMemory()) { it.concepts.size }
                },
                render = { context, modifier -> DebugWorkspaceUiStatePanel(context, modifier) }
            )
        )
        register(
            DebugWorkspacePanel(
                id = builtin("registries"),
                icon = builtinIcon("globe"),
                labelKey = "debug:workspace.nav.registries.label",
                descriptionKey = "debug:workspace.nav.registries.description",
                count = { context ->
                    rememberMemoryValue(context.registryMemory()) { snapshot ->
                        snapshot.registries.values.sumOf { it.size }
                    }
                },
                render = { context, modifier -> DebugWorkspaceRegistriesPanel(context, modifier) }
            )
        )
        register(
            DebugWorkspacePanel(
                id = builtin("pending"),
                icon = builtinIcon("reload"),
                labelKey = "debug:workspace.nav.pending.label",
                descriptionKey = "debug:workspace.nav.pending.description",
                count = { context ->
                    rememberMemoryValue(context.gateway.pendingActionsMemory()) { it.size }
                },
                render = { context, modifier -> DebugWorkspacePendingPanel(context, modifier) }
            )
        )
    }

    fun register(panel: DebugWorkspacePanel) {
        require(panels.putIfAbsent(panel.id, panel) == null) {
            "Debug workspace panel already registered for ${panel.id}"
        }
    }

    fun all(): List<DebugWorkspacePanel> = panels.values.toList()

    fun get(id: Identifier): DebugWorkspacePanel? = panels[id]

    fun first(): Identifier = panels.keys.firstOrNull()
        ?: error("No debug workspace panels registered")
}

private fun builtin(path: String): Identifier =
    Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, path)

private fun builtinIcon(name: String): Identifier =
    Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/$name.svg")
