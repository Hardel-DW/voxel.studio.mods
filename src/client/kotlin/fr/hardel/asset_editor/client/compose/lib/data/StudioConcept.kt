package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioConceptSpec
import fr.hardel.asset_editor.client.navigation.StudioEditorTab
import fr.hardel.asset_editor.permission.StudioPermissions
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

enum class StudioConcept(
    val registryKey: ResourceKey<out Registry<*>>,
    val titleKey: String,
    val icon: Identifier,
    override val defaultEditorTab: StudioEditorTab,
    val tabs: List<StudioTabDefinition>
) : StudioConceptSpec {
    ENCHANTMENT(
        Registries.ENCHANTMENT,
        "studio.concept.enchantment",
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/enchantment.png"),
        StudioEditorTab.MAIN,
        listOf(
            StudioTabDefinition("global", "enchantment:section.global", StudioEditorTab.MAIN),
            StudioTabDefinition("find", "enchantment:section.find", StudioEditorTab.FIND),
            StudioTabDefinition("slots", "enchantment:section.slots", StudioEditorTab.SLOTS),
            StudioTabDefinition("items", "enchantment:section.supported", StudioEditorTab.ITEMS),
            StudioTabDefinition("exclusive", "enchantment:section.exclusive", StudioEditorTab.EXCLUSIVE),
            StudioTabDefinition("technical", "enchantment:section.technical", StudioEditorTab.TECHNICAL)
        )
    ),
    LOOT_TABLE(
        Registries.LOOT_TABLE,
        "studio.concept.loot_table",
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/loot_table.png"),
        StudioEditorTab.MAIN,
        listOf(
            StudioTabDefinition("main", "loot:section.main", StudioEditorTab.MAIN),
            StudioTabDefinition("pools", "loot:section.pools", StudioEditorTab.POOLS)
        )
    ),
    RECIPE(
        Registries.RECIPE,
        "studio.concept.recipe",
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/recipe.png"),
        StudioEditorTab.MAIN,
        listOf(
            StudioTabDefinition("main", "recipe:section.main", StudioEditorTab.MAIN)
        )
    ),
    STRUCTURE(
        Registries.STRUCTURE,
        "studio.concept.structure",
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/structure.png"),
        StudioEditorTab.MAIN,
        listOf()
    );

    override val concept: StudioConcept
        get() = this

    override val supportedTabs: ImmutableSet<StudioEditorTab>
        get() = tabs.map(StudioTabDefinition::tab).toImmutableSet()

    override fun overview(): ConceptOverviewDestination =
        ConceptOverviewDestination(this)

    override fun changes(): ConceptChangesDestination =
        ConceptChangesDestination(this)

    override fun editor(
        elementId: String,
        tab: StudioEditorTab
    ): ElementEditorDestination =
        ElementEditorDestination(this, elementId, tab)

    fun registry(): String = registryKey.identifier().path

    companion object {
        @JvmStatic
        fun byRegistry(registry: String): StudioConcept {
            for (concept in entries) {
                if (concept.registry() == registry) {
                    return concept
                }
            }
            return ENCHANTMENT
        }

        @JvmStatic
        fun firstAccessible(permissions: StudioPermissions): StudioConcept? {
            if (permissions.isNone) {
                return null
            }

            for (concept in entries) {
                if (concept == STRUCTURE) {
                    continue
                }
                return concept
            }
            return null
        }
    }
}
