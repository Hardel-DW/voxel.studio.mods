package fr.hardel.asset_editor.studio

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.memory.ui.ConceptUiSnapshot
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

private data class StudioLayoutBinding(
    val defaultSnapshot: ConceptUiSnapshot,
    val supportsSimulation: Boolean,
    val prefetchAtlas: Boolean,
    val render: @Composable (StudioContext) -> Unit
)

object StudioUiRegistry {
    private val layouts = LinkedHashMap<Identifier, StudioLayoutBinding>()
    private val pages = LinkedHashMap<Pair<Identifier, Identifier>, @Composable (StudioContext) -> Unit>()

    @JvmStatic
    fun registerLayout(
        registryId: Identifier,
        defaultSnapshot: ConceptUiSnapshot = ConceptUiSnapshot(),
        supportsSimulation: Boolean = false,
        prefetchAtlas: Boolean = false,
        render: @Composable (StudioContext) -> Unit
    ) {
        layouts[registryId] = StudioLayoutBinding(defaultSnapshot, supportsSimulation, prefetchAtlas, render)
    }

    @JvmStatic
    fun registerPage(
        registryId: Identifier,
        tabId: Identifier,
        render: @Composable (StudioContext) -> Unit
    ) {
        pages[registryId to tabId] = render
    }

    @JvmStatic
    fun supportedConceptIds(): List<Identifier> =
        StudioRegistryResolver.conceptIds(currentRegistries()).filter(::hasLayout)

    @JvmStatic
    fun firstSupportedConceptId(): Identifier? =
        supportedConceptIds().firstOrNull()

    @JvmStatic
    fun hasLayout(conceptId: Identifier): Boolean =
        layoutBinding(conceptId) != null

    @JvmStatic
    fun supportsSimulation(conceptId: Identifier): Boolean =
        layoutBinding(conceptId)?.supportsSimulation == true

    @JvmStatic
    fun shouldPrefetchAtlas(conceptId: Identifier): Boolean =
        layoutBinding(conceptId)?.prefetchAtlas == true

    @JvmStatic
    fun defaultSnapshot(conceptId: Identifier): ConceptUiSnapshot =
        layoutBinding(conceptId)?.defaultSnapshot ?: ConceptUiSnapshot()

    @Composable
    fun renderLayout(context: StudioContext, conceptId: Identifier): Boolean {
        val binding = layoutBinding(conceptId) ?: return false
        binding.render(context)
        return true
    }

    @Composable
    fun renderPage(context: StudioContext, destination: ElementEditorDestination): Boolean {
        val registryId = StudioRegistryResolver.requireConceptDefinition(currentRegistries(), destination.conceptId).registry()
        val render = pages[registryId to destination.tabId] ?: return false
        render(context)
        return true
    }

    private fun layoutBinding(conceptId: Identifier): StudioLayoutBinding? {
        val definition = StudioRegistryResolver.conceptDefinition(currentRegistries(), conceptId) ?: return null
        return layouts[definition.registry()]
    }

    private fun currentRegistries() =
        Minecraft.getInstance().connection?.registryAccess()
}
