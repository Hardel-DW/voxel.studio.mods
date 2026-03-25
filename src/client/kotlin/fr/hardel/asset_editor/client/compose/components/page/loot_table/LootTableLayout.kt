package fr.hardel.asset_editor.client.compose.components.page.loot_table

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.ui.tree.buildConceptTreeState
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.navigation.StudioEditorTab
import fr.hardel.asset_editor.client.compose.routes.loot.LootTableMainPage
import fr.hardel.asset_editor.client.compose.routes.loot.LootTableOverviewPage

@Composable
fun LootTableLayout(context: StudioContext) {
    val concept = StudioConcept.LOOT_TABLE
    val conceptUi = rememberConceptUi(context, concept)
    val currentEditor = rememberCurrentElementDestination(context, concept)
    val treeState = buildConceptTreeState(
        concept = concept,
        tree = LootTableTreeBuilder.build(emptyList()),
        filterPath = conceptUi.filterPath,
        selectedElementId = currentEditor?.elementId,
        expandedTreePaths = conceptUi.expandedTreePaths,
        elementIcon = concept.icon,
        folderIcons = emptyMap(),
        disableAutoExpand = false,
        onSelectAll = {
            context.uiState().updateFilterPath(concept, "")
            context.navigationState().navigate(concept.overview())
        },
        onSelectFolder = { path ->
            context.uiState().updateFilterPath(concept, path)
            context.navigationState().navigate(concept.overview())
        },
        onSelectElement = { elementId ->
            context.navigationState().openElement(concept.editor(elementId, StudioEditorTab.MAIN))
        },
        onToggleExpanded = { path, expanded ->
            context.uiState().setTreeExpanded(concept, path, expanded)
        },
        onNavigateChanges = { context.navigationState().navigate(concept.changes()) }
    )

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            concept = concept,
            icon = concept.icon,
            sidebarTitleKey = "loot:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                when (destination) {
                    is ConceptOverviewDestination -> LootTableOverviewPage(context)
                    is ElementEditorDestination -> LootTableMainPage()
                    else -> EmptyPage()
                }
            },
            sidebarExtras = emptyList()
        )
    )
}
