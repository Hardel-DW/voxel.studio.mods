package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioConceptSpec
import fr.hardel.asset_editor.client.navigation.StudioEditorTab
import fr.hardel.asset_editor.client.navigation.StudioEditorTabs
import fr.hardel.asset_editor.permission.StudioPermissions
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

data class StudioConcept(
    val id: Identifier
) : StudioConceptSpec {
    private val definition get() = StudioRegistryAccess.requireConceptDefinition(id)

    @Suppress("UNCHECKED_CAST")
    val registryKey: ResourceKey<out Registry<*>>
        get() = definition.registryKey() as ResourceKey<out Registry<*>>

    val titleKey: String
        get() = "studio.concept.${registry()}"

    val icon: Identifier
        get() = Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/${registry()}.png")

    val tabs: List<StudioEditorTab>
        get() {
            val explicitTabs = definition.tabIds()
            if (explicitTabs.isNotEmpty()) {
                return explicitTabs.mapNotNull(StudioEditorTabs::byId)
            }

            val tabsTagKey = definition.tabsTagKey() ?: return emptyList()
            return StudioRegistryAccess.resolveEditorTabs(tabsTagKey)
        }

    override val concept: StudioConcept
        get() = this

    override val defaultEditorTab: StudioEditorTab
        get() = StudioEditorTabs.require(definition.defaultEditorTab())

    override val supportedTabs: ImmutableSet<StudioEditorTab>
        get() = tabs.toImmutableSet()

    override fun overview(): ConceptOverviewDestination =
        ConceptOverviewDestination(this)

    override fun changes(): ConceptChangesDestination =
        ConceptChangesDestination(this)

    override fun editor(
        elementId: String,
        tab: StudioEditorTab
    ): ElementEditorDestination =
        ElementEditorDestination(this, elementId, tab)

    fun registry(): String = definition.registry().path

    fun tabTranslationKey(tab: StudioEditorTab): String =
        "studio.concept.${registry()}.tab.${tab.path()}"
}

object StudioConcepts {
    @JvmStatic
    fun entries(): List<StudioConcept> =
        StudioRegistryAccess.concepts()

    @JvmStatic
    fun byId(id: Identifier): StudioConcept? =
        StudioRegistryAccess.concept(id)

    @JvmStatic
    fun require(id: Identifier): StudioConcept =
        byId(id) ?: error("Unknown studio concept '$id'")

    @JvmStatic
    fun byRegistry(registry: String): StudioConcept? =
        entries().firstOrNull { it.registry() == registry }

    @JvmStatic
    fun requireByRegistry(registry: String): StudioConcept =
        byRegistry(registry) ?: error("Unknown studio concept registry '$registry'")

    @JvmStatic
    fun requireByRegistryKey(registryKey: ResourceKey<out Registry<*>>): StudioConcept =
        entries().firstOrNull { it.registryKey == registryKey }
            ?: error("Unknown studio concept registry key '${registryKey.identifier()}'")

    @JvmStatic
    fun firstAccessible(permissions: StudioPermissions): StudioConcept? {
        if (permissions.isNone) {
            return null
        }
        return entries().firstOrNull()
    }
}
