package fr.hardel.asset_editor.client.bridge

import fr.hardel.asset_editor.client.ClientSessionDispatch
import fr.hardel.asset_editor.client.compose.lib.action.EditorActionResult
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView
import fr.hardel.asset_editor.client.compose.lib.data.StudioViewMode
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionGateway
import fr.hardel.asset_editor.client.state.ClientSessionState
import fr.hardel.asset_editor.client.state.ClientWorkspaceState
import fr.hardel.asset_editor.client.state.StudioOpenTab
import fr.hardel.asset_editor.client.state.WorkspaceTabsState
import fr.hardel.asset_editor.client.state.WorkspaceUiState
import fr.hardel.asset_editor.workspace.action.EditorAction
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

class ComposeWorkspaceGateway(
    sessionState: ClientSessionState,
    workspaceState: ClientWorkspaceState
) {
    private val delegate = EditorActionGateway(sessionState, workspaceState)

    fun attach(dispatch: ClientSessionDispatch) {
        dispatch.setGateway(delegate)
    }

    fun detach(dispatch: ClientSessionDispatch) {
        dispatch.clearGateway(delegate)
    }

    fun <T : Any> dispatch(
        registry: ResourceKey<Registry<T>>,
        target: Identifier?,
        action: EditorAction
    ): EditorActionResult {
        val result = delegate.dispatch(registry, target, action)
        return EditorActionResult(
            status = EditorActionResult.Status.valueOf(result.status().name),
            message = result.message()
        )
    }

    fun requestPackWorkspace(registry: ResourceKey<*>) {
        delegate.requestPackWorkspace(registry)
    }
}

fun WorkspaceTabsState.openComposeElement(elementId: String, route: StudioRoute) {
    openElement(elementId, route.toLegacyRoute())
}

fun WorkspaceUiState.setComposeSidebarView(view: StudioSidebarView) {
    setSidebarView(view.toLegacySidebarView())
}

fun WorkspaceUiState.setComposeViewMode(mode: StudioViewMode) {
    setViewMode(mode.toLegacyViewMode())
}

fun StudioOpenTab.toComposeOpenTab(): StudioContext.OpenTab =
    StudioContext.OpenTab(elementId(), route().toComposeRoute())

fun StudioOpenTab.toComposeRoute(): StudioRoute =
    route().toComposeRoute()

fun legacyViewModeToCompose(mode: fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode): StudioViewMode =
    when (mode) {
        fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode.GRID -> StudioViewMode.GRID
        fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode.LIST -> StudioViewMode.LIST
    }

fun legacySidebarViewToCompose(view: fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView): StudioSidebarView =
    when (view) {
        fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView.SLOTS -> StudioSidebarView.SLOTS
        fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView.ITEMS -> StudioSidebarView.ITEMS
        fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView.EXCLUSIVE -> StudioSidebarView.EXCLUSIVE
    }

private fun StudioRoute.toLegacyRoute(): fr.hardel.asset_editor.client.javafx.routes.StudioRoute =
    when (this) {
        StudioRoute.EnchantmentOverview -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_OVERVIEW
        StudioRoute.EnchantmentMain -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_MAIN
        StudioRoute.EnchantmentFind -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_FIND
        StudioRoute.EnchantmentSlots -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_SLOTS
        StudioRoute.EnchantmentItems -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_ITEMS
        StudioRoute.EnchantmentExclusive -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_EXCLUSIVE
        StudioRoute.EnchantmentTechnical -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_TECHNICAL
        StudioRoute.EnchantmentSimulation -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_SIMULATION
        StudioRoute.LootTableOverview -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_OVERVIEW
        StudioRoute.LootTableMain -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_MAIN
        StudioRoute.LootTablePools -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_POOLS
        StudioRoute.RecipeOverview -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.RECIPE_OVERVIEW
        StudioRoute.RecipeMain -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.RECIPE_MAIN
        StudioRoute.ChangesMain -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.CHANGES_MAIN
        StudioRoute.Debug -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.DEBUG
        StudioRoute.NoPermission -> fr.hardel.asset_editor.client.javafx.routes.StudioRoute.NO_PERMISSION
    }

private fun fr.hardel.asset_editor.client.javafx.routes.StudioRoute.toComposeRoute(): StudioRoute =
    when (this) {
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_OVERVIEW -> StudioRoute.EnchantmentOverview
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_MAIN -> StudioRoute.EnchantmentMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_FIND -> StudioRoute.EnchantmentFind
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_SLOTS -> StudioRoute.EnchantmentSlots
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_ITEMS -> StudioRoute.EnchantmentItems
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_EXCLUSIVE -> StudioRoute.EnchantmentExclusive
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_TECHNICAL -> StudioRoute.EnchantmentTechnical
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_SIMULATION -> StudioRoute.EnchantmentSimulation
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_OVERVIEW -> StudioRoute.LootTableOverview
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_MAIN -> StudioRoute.LootTableMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_POOLS -> StudioRoute.LootTablePools
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.RECIPE_OVERVIEW -> StudioRoute.RecipeOverview
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.RECIPE_MAIN -> StudioRoute.RecipeMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.CHANGES_MAIN -> StudioRoute.ChangesMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.DEBUG -> StudioRoute.Debug
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.NO_PERMISSION -> StudioRoute.NoPermission
    }

private fun StudioSidebarView.toLegacySidebarView(): fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView =
    when (this) {
        StudioSidebarView.SLOTS -> fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView.SLOTS
        StudioSidebarView.ITEMS -> fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView.ITEMS
        StudioSidebarView.EXCLUSIVE -> fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView.EXCLUSIVE
    }

private fun StudioViewMode.toLegacyViewMode(): fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode =
    when (this) {
        StudioViewMode.GRID -> fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode.GRID
        StudioViewMode.LIST -> fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode.LIST
    }
