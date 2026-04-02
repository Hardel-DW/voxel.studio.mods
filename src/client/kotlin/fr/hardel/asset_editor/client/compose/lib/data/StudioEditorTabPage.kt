package fr.hardel.asset_editor.client.compose.lib.data

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import net.minecraft.resources.Identifier
import java.util.ServiceLoader

interface StudioEditorTabPage {
    val conceptId: Identifier
    val tabId: Identifier

    @Composable
    fun Render(context: StudioContext)
}

object StudioEditorTabPages {
    private val pages: Map<String, StudioEditorTabPage> by lazy {
        val loaded = LinkedHashMap<String, StudioEditorTabPage>()
        ServiceLoader.load(StudioEditorTabPage::class.java).forEach { page ->
            val key = key(page.conceptId, page.tabId)
            require(loaded.putIfAbsent(key, page) == null) {
                "Duplicate studio editor tab page registration: $key"
            }
        }
        loaded
    }

    @Composable
    fun Render(context: StudioContext, destination: ElementEditorDestination): Boolean {
        val page = pages[key(destination.concept.id, destination.tab.id)] ?: return false
        page.Render(context)
        return true
    }

    private fun key(conceptId: Identifier, tabId: Identifier): String =
        conceptId.toString() + "|" + tabId
}

abstract class BaseStudioEditorTabPage(
    conceptPath: String,
    tabPath: String
) : StudioEditorTabPage {
    final override val conceptId: Identifier =
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, conceptPath)

    final override val tabId: Identifier =
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, tabPath)
}
