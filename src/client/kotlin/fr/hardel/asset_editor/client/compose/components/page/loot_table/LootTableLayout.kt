package fr.hardel.asset_editor.client.compose.components.page.loot_table

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeController
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.loot.LootTableMainPage
import fr.hardel.asset_editor.client.compose.routes.loot.LootTableOverviewPage

@Composable
fun LootTableLayout(context: StudioContext) {
    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            concept = StudioConcept.LOOT_TABLE,
            icon = StudioConcept.LOOT_TABLE.icon,
            sidebarTitleKey = "loot:overview.title",
            treeConfig = TreeController.Config(
                overviewRoute = StudioRoute.LootTableOverview,
                detailRoute = StudioRoute.LootTableMain,
                changesRoute = StudioRoute.ChangesMain,
                concept = StudioConcept.LOOT_TABLE.registry(),
                tabRoutes = StudioConcept.LOOT_TABLE.tabRoutes(),
                tree = LootTableTreeBuilder.build(emptyList()),
                elementIcon = StudioConcept.LOOT_TABLE.icon,
                filterPath = { context.filterPath },
                setFilterPath = { value -> context.uiState().setFilterPath(value) },
                currentElementId = { context.currentElementId },
                setCurrentElementId = { value -> context.tabsState().setCurrentElementId(value ?: "") },
                onOpenElement = { elementId, _ ->
                    context.tabsState().setCurrentElementId(elementId)
                    context.router.navigate(StudioRoute.LootTableMain)
                }
            ),
            simulationRoute = null,
            showViewModeToggle = false,
            pageFactory = { route ->
                when (route) {
                    StudioRoute.LootTableOverview -> LootTableOverviewPage(context)
                    StudioRoute.LootTableMain, StudioRoute.LootTablePools -> LootTableMainPage()
                    else -> EmptyPage()
                }
            },
            sidebarExtras = emptyList()
        )
    )
}
