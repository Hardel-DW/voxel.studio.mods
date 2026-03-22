package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeController
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.recipe.RecipeMainPage
import fr.hardel.asset_editor.client.compose.routes.recipe.RecipeOverviewPage

@Composable
fun RecipeLayout(context: StudioContext) {
    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            concept = StudioConcept.RECIPE,
            icon = StudioConcept.RECIPE.icon,
            sidebarTitleKey = "recipe:overview.title",
            treeConfig = TreeController.Config(
                overviewRoute = StudioRoute.RecipeOverview,
                detailRoute = StudioRoute.RecipeMain,
                changesRoute = StudioRoute.ChangesMain,
                concept = StudioConcept.RECIPE.registry(),
                tabRoutes = StudioConcept.RECIPE.tabRoutes(),
                tree = RecipeTreeBuilder.build(emptyList()),
                elementIcon = StudioConcept.RECIPE.icon,
                folderIcons = RecipeTreeBuilder.folderIcons(),
                filterPath = { context.filterPath },
                setFilterPath = { value -> context.uiState().setFilterPath(value) },
                currentElementId = { context.currentElementId },
                setCurrentElementId = { value -> context.tabsState().setCurrentElementId(value ?: "") },
                onOpenElement = { elementId, _ ->
                    context.tabsState().setCurrentElementId(elementId)
                    context.router.navigate(StudioRoute.RecipeMain)
                }
            ),
            simulationRoute = null,
            showViewModeToggle = false,
            pageFactory = { route ->
                when (route) {
                    StudioRoute.RecipeOverview -> RecipeOverviewPage(context)
                    StudioRoute.RecipeMain -> RecipeMainPage()
                    else -> EmptyPage()
                }
            },
            sidebarExtras = emptyList()
        )
    )
}
