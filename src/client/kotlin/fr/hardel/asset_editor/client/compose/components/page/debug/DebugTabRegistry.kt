package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.routes.debug.DebugCodeBlockPage
import fr.hardel.asset_editor.client.compose.routes.debug.DebugLogsPage
import fr.hardel.asset_editor.client.compose.routes.debug.DebugNetworkPage
import fr.hardel.asset_editor.client.compose.routes.debug.DebugRenderPage
import fr.hardel.asset_editor.client.compose.routes.debug.DebugWorkspacePage
import net.minecraft.resources.Identifier

typealias DebugTabRenderer = @Composable (StudioContext) -> Unit

class DebugTab(
    val id: Identifier,
    val labelKey: String,
    val render: DebugTabRenderer
)

/**
 * Registry of tabs shown in the top bar of the Debug layout.
 *
 * Downstream mods register their own tab by calling [register] during client init:
 *
 *     DebugTabRegistry.register(
 *         DebugTab(
 *             id = Identifier.fromNamespaceAndPath("mymod", "profiler"),
 *             labelKey = "debug:layout.tab.mymod.profiler",
 *             render = { context -> MyProfilerPage(context) }
 *         )
 *     )
 *
 * The tab id must be unique. Order of registration is preserved in the layout.
 */
object DebugTabRegistry {

    private val tabs = linkedMapOf<Identifier, DebugTab>()

    init {
        register(
            DebugTab(
                id = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "workspace"),
                labelKey = "debug:layout.tab.workspace",
                render = { context -> DebugWorkspacePage(context) }
            )
        )
        register(
            DebugTab(
                id = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "code"),
                labelKey = "debug:layout.tab.code",
                render = { DebugCodeBlockPage() }
            )
        )
        register(
            DebugTab(
                id = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "render"),
                labelKey = "debug:layout.tab.render",
                render = { DebugRenderPage() }
            )
        )
        register(
            DebugTab(
                id = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "logs"),
                labelKey = "debug:layout.tab.logs",
                render = { context -> DebugLogsPage(context) }
            )
        )
        register(
            DebugTab(
                id = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "network"),
                labelKey = "debug:layout.tab.network",
                render = { context -> DebugNetworkPage(context) }
            )
        )
    }

    fun register(tab: DebugTab) {
        require(tabs.putIfAbsent(tab.id, tab) == null) {
            "Debug tab already registered for ${tab.id}"
        }
    }

    fun all(): List<DebugTab> = tabs.values.toList()

    fun get(id: Identifier): DebugTab? = tabs[id]

    fun first(): Identifier = tabs.keys.firstOrNull()
        ?: error("No debug tabs registered")
}