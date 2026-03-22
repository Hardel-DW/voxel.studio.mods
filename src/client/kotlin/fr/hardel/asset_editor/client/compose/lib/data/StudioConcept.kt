package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.permission.StudioPermissions
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

enum class StudioConcept(
    val registryKey: ResourceKey<out Registry<*>>,
    val titleKey: String,
    val overviewRoute: StudioRoute,
    val icon: Identifier,
    val tabs: List<StudioTabDefinition>
) {
    ENCHANTMENT(
        Registries.ENCHANTMENT,
        "studio.concept.enchantment",
        StudioRoute.EnchantmentOverview,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/enchantment.png"),
        listOf(
            StudioTabDefinition("global", "enchantment:section.global", StudioRoute.EnchantmentMain),
            StudioTabDefinition("find", "enchantment:section.find", StudioRoute.EnchantmentFind),
            StudioTabDefinition("slots", "enchantment:section.slots", StudioRoute.EnchantmentSlots),
            StudioTabDefinition("items", "enchantment:section.supported", StudioRoute.EnchantmentItems),
            StudioTabDefinition("exclusive", "enchantment:section.exclusive", StudioRoute.EnchantmentExclusive),
            StudioTabDefinition("technical", "enchantment:section.technical", StudioRoute.EnchantmentTechnical)
        )
    ),
    LOOT_TABLE(
        Registries.LOOT_TABLE,
        "studio.concept.loot_table",
        StudioRoute.LootTableOverview,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/loot_table.png"),
        listOf(
            StudioTabDefinition("main", "loot:section.main", StudioRoute.LootTableMain),
            StudioTabDefinition("pools", "loot:section.pools", StudioRoute.LootTablePools)
        )
    ),
    RECIPE(
        Registries.RECIPE,
        "studio.concept.recipe",
        StudioRoute.RecipeOverview,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/recipe.png"),
        listOf(
            StudioTabDefinition("main", "recipe:section.main", StudioRoute.RecipeMain)
        )
    ),
    STRUCTURE(
        Registries.STRUCTURE,
        "studio.concept.structure",
        StudioRoute.EnchantmentOverview,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/structure.png"),
        listOf()
    );

    fun registry(): String = registryKey.identifier().path

    fun tabRoutes(): List<StudioRoute> = tabs.map(StudioTabDefinition::route)

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
        fun byRoute(route: StudioRoute): StudioConcept =
            byRegistry(route.concept())

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
