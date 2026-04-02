package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.navigation.StudioEditorTab
import fr.hardel.asset_editor.studio.StudioConceptDef
import fr.hardel.asset_editor.studio.StudioEditorTabDef
import fr.hardel.asset_editor.studio.StudioRegistries
import net.minecraft.client.Minecraft
import net.minecraft.tags.TagKey

object StudioRegistryAccess {
    fun concepts(): List<StudioConcept> =
        conceptDefinitions()
            .sortedBy { it.first.toString() }
            .map { (id, _) -> StudioConcept(id) }

    fun concept(id: net.minecraft.resources.Identifier): StudioConcept? =
        conceptDefinitions().firstOrNull { it.first == id }?.let { StudioConcept(it.first) }

    fun requireConceptDefinition(id: net.minecraft.resources.Identifier): StudioConceptDef =
        conceptDefinitions().firstOrNull { it.first == id }?.second
            ?: error("Unknown studio concept '$id'")

    fun editorTabs(): List<StudioEditorTab> =
        editorTabDefinitions()
            .sortedBy { it.toString() }
            .map(::StudioEditorTab)

    fun editorTab(id: net.minecraft.resources.Identifier): StudioEditorTab? =
        editorTabDefinitions().firstOrNull { it == id }?.let(::StudioEditorTab)

    fun resolveEditorTabs(tagKey: TagKey<StudioEditorTabDef>): List<StudioEditorTab> {
        val connection = Minecraft.getInstance().connection ?: return emptyList()
        val lookup = connection.registryAccess().lookup(StudioRegistries.STUDIO_TAB).orElse(null) ?: return emptyList()
        val tag = lookup.get(tagKey).orElse(null) ?: return emptyList()
        return tag.stream().toList()
            .mapNotNull { holder -> holder.unwrapKey().map { StudioEditorTab(it.identifier()) }.orElse(null) }
            .toList()
    }

    private fun conceptDefinitions(): List<Pair<net.minecraft.resources.Identifier, StudioConceptDef>> {
        val connection = Minecraft.getInstance().connection ?: return emptyList()
        val lookup = connection.registryAccess().lookup(StudioRegistries.STUDIO_CONCEPT).orElse(null) ?: return emptyList()
        return lookup.listElements().toList()
            .mapNotNull { holder ->
                holder.unwrapKey().map { it.identifier() to holder.value() }.orElse(null)
            }
            .toList()
    }

    private fun editorTabDefinitions(): List<net.minecraft.resources.Identifier> {
        val connection = Minecraft.getInstance().connection ?: return emptyList()
        val lookup = connection.registryAccess().lookup(StudioRegistries.STUDIO_TAB).orElse(null) ?: return emptyList()
        return lookup.listElements().toList()
            .mapNotNull { holder -> holder.unwrapKey().map { it.identifier() }.orElse(null) }
            .toList()
    }
}
