package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.rememberRecipeEntries
import fr.hardel.asset_editor.client.compose.components.ui.tree.buildConceptTreeState
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.session.StudioDataSlots
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.compose.lib.ConceptChangesDestination
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioDestination
import fr.hardel.asset_editor.client.compose.routes.recipe.RecipeOverviewPage
import kotlinx.collections.immutable.toImmutableSet
import net.minecraft.core.registries.Registries

@Composable
fun RecipeLayout(context: StudioContext) {
    val conceptId = context.studioConceptId(Registries.RECIPE) ?: return
    val conceptUi = rememberConceptUi(context, conceptId)
    val entries = rememberRecipeEntries(context)
    val recipeEntryDefs = rememberServerData(StudioDataSlots.RECIPE_ENTRIES)
    val currentEditor = rememberCurrentElementDestination(context, conceptId)
    val conceptIcon = remember(conceptId) { context.studioIcon(conceptId) }
    val defaultEditorTab = remember(conceptId) { context.studioDefaultEditorTab(conceptId) }
    val tree = remember(entries, recipeEntryDefs) {
        RecipeTreeBuilder.build(entries.map { RecipeTreeBuilder.RecipeEntry(it.id.toString(), it.type) })
    }
    val treeState = buildConceptTreeState(
        conceptId = conceptId,
        tree = tree,
        filterPath = conceptUi.filterPath,
        selectedElementId = currentEditor?.elementId,
        expandedTreePaths = conceptUi.expandedTreePaths.toImmutableSet(),
        elementIcon = conceptIcon,
        folderIcons = RecipeTreeBuilder.folderIcons(),
        disableAutoExpand = false,
        onSelectAll = {
            context.uiMemory().updateFilterPath(conceptId, "")
            context.navigationMemory().navigate(ConceptOverviewDestination(conceptId))
        },
        onSelectFolder = { path ->
            context.uiMemory().updateFilterPath(conceptId, path)
            context.navigationMemory().navigate(ConceptOverviewDestination(conceptId))
        },
        onSelectElement = { elementId ->
            context.navigationMemory().openElement(ElementEditorDestination(conceptId, elementId, defaultEditorTab))
        },
        onToggleExpanded = { path, expanded ->
            context.uiMemory().setTreeExpanded(conceptId, path, expanded)
        },
        onNavigateChanges = { context.navigationMemory().navigate(ConceptChangesDestination(conceptId)) }
    )

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            conceptId = conceptId,
            registryKey = Registries.RECIPE,
            icon = conceptIcon,
            sidebarTitleKey = "recipe:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                when (destination) {
                    is ConceptOverviewDestination -> RecipeOverviewPage(context)
                    is ElementEditorDestination -> if (!StudioUiRegistry.renderPage(context, destination)) EmptyPage()
                    else -> EmptyPage()
                }
            },
            sidebarExtras = emptyList()
        )
    )
}
