package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.rememberRecipeEntries
import fr.hardel.asset_editor.client.compose.components.ui.tree.buildConceptTreeState
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcepts
import fr.hardel.asset_editor.client.compose.lib.data.BaseStudioConceptRenderer
import fr.hardel.asset_editor.client.compose.lib.data.StudioEditorTabPages
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.compose.routes.recipe.RecipeOverviewPage
import kotlinx.collections.immutable.toImmutableSet
import net.minecraft.core.registries.Registries

@Composable
fun RecipeLayout(context: StudioContext) {
    val concept = StudioConcepts.requireByRegistryKey(Registries.RECIPE)
    val conceptUi = rememberConceptUi(context, concept)
    val entries = rememberRecipeEntries(context)
    val currentEditor = rememberCurrentElementDestination(context, concept)
    val tree = remember(entries) {
        RecipeTreeBuilder.build(entries.map { RecipeTreeBuilder.RecipeEntry(it.id.toString(), it.type) })
    }
    val treeState = buildConceptTreeState(
        concept = concept,
        tree = tree,
        filterPath = conceptUi.filterPath,
        selectedElementId = currentEditor?.elementId,
        expandedTreePaths = conceptUi.expandedTreePaths.toImmutableSet(),
        elementIcon = concept.icon,
        folderIcons = RecipeTreeBuilder.folderIcons(),
        disableAutoExpand = false,
        onSelectAll = {
            context.uiMemory().updateFilterPath(concept, "")
            context.navigationMemory().navigate(concept.overview())
        },
        onSelectFolder = { path ->
            context.uiMemory().updateFilterPath(concept, path)
            context.navigationMemory().navigate(concept.overview())
        },
        onSelectElement = { elementId ->
            context.navigationMemory().openElement(concept.editor(elementId, concept.defaultEditorTab))
        },
        onToggleExpanded = { path, expanded ->
            context.uiMemory().setTreeExpanded(concept, path, expanded)
        },
        onNavigateChanges = { context.navigationMemory().navigate(concept.changes()) }
    )

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            concept = concept,
            icon = concept.icon,
            sidebarTitleKey = "recipe:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                when (destination) {
                    is ConceptOverviewDestination -> RecipeOverviewPage(context)
                    is ElementEditorDestination -> if (!StudioEditorTabPages.Render(context, destination)) EmptyPage()
                    else -> EmptyPage()
                }
            },
            sidebarExtras = emptyList()
        )
    )
}

class RecipeStudioConceptRenderer : BaseStudioConceptRenderer(
    conceptPath = "recipe",
    shouldPrefetchAtlas = true
) {
    @Composable
    override fun Render(context: StudioContext) {
        RecipeLayout(context)
    }
}
