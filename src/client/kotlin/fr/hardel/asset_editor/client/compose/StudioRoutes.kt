package fr.hardel.asset_editor.client.compose

import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry
import fr.hardel.asset_editor.client.compose.components.page.enchantment.EnchantmentLayout
import fr.hardel.asset_editor.client.compose.components.page.loot_table.LootTableLayout
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeLayout
import fr.hardel.asset_editor.client.compose.components.page.enchantment.StudioSidebarView
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentExclusivePage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentFindPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentItemsPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentMainPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentSlotsPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentTechnicalPage
import fr.hardel.asset_editor.client.compose.routes.loot.LootTableMainPage
import fr.hardel.asset_editor.client.compose.routes.loot.LootTablePoolsPage
import fr.hardel.asset_editor.client.compose.routes.recipe.RecipeMainPage
import fr.hardel.asset_editor.client.memory.session.ui.ConceptUiSnapshot
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

fun registerStudioRoutes() {
    StudioUiRegistry.registerLayout(
        registryId = Registries.ENCHANTMENT.identifier(),
        defaultSnapshot = ConceptUiSnapshot("", "", StudioSidebarView.SLOTS, emptyMap(), true),
        supportsSimulation = true,
        prefetchAtlas = true,
        render = { context -> EnchantmentLayout(context) }
    )
    StudioUiRegistry.registerLayout(
        registryId = Registries.LOOT_TABLE.identifier(),
        render = { context -> LootTableLayout(context) }
    )
    StudioUiRegistry.registerLayout(
        registryId = Registries.RECIPE.identifier(),
        prefetchAtlas = true,
        render = { context -> RecipeLayout(context) }
    )

    StudioUiRegistry.registerPage(
        registryId = Registries.ENCHANTMENT.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "main"),
        render = { context -> EnchantmentMainPage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.ENCHANTMENT.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "find"),
        render = { context -> EnchantmentFindPage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.ENCHANTMENT.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "slots"),
        render = { context -> EnchantmentSlotsPage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.ENCHANTMENT.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "items"),
        render = { context -> EnchantmentItemsPage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.ENCHANTMENT.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "exclusive"),
        render = { context -> EnchantmentExclusivePage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.ENCHANTMENT.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "technical"),
        render = { context -> EnchantmentTechnicalPage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.LOOT_TABLE.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "main"),
        render = { context -> LootTableMainPage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.LOOT_TABLE.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "pools"),
        render = { context -> LootTablePoolsPage(context) }
    )
    StudioUiRegistry.registerPage(
        registryId = Registries.RECIPE.identifier(),
        tabId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "main"),
        render = { context -> RecipeMainPage(context) }
    )
}
