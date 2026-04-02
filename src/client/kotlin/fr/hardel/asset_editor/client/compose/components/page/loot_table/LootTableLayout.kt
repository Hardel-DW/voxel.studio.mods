package fr.hardel.asset_editor.client.compose.components.page.loot_table

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
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
import net.minecraft.core.registries.Registries
import fr.hardel.asset_editor.client.compose.routes.loot.LootTableOverviewPage
import kotlinx.collections.immutable.toImmutableSet

@Composable
fun LootTableLayout(context: StudioContext) {
    val concept = StudioConcepts.requireByRegistryKey(Registries.LOOT_TABLE)
    val conceptUi = rememberConceptUi(context, concept)
    val currentEditor = rememberCurrentElementDestination(context, concept)
    val treeState = buildConceptTreeState(
        concept = concept,
        tree = LootTableTreeBuilder.build(emptyList()),
        filterPath = conceptUi.filterPath,
        selectedElementId = currentEditor?.elementId,
        expandedTreePaths = conceptUi.expandedTreePaths.toImmutableSet(),
        elementIcon = concept.icon,
        folderIcons = emptyMap(),
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
            sidebarTitleKey = "loot:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                when (destination) {
                    is ConceptOverviewDestination -> LootTableOverviewPage(context)
                    is ElementEditorDestination -> if (!StudioEditorTabPages.Render(context, destination)) EmptyPage()
                    else -> EmptyPage()
                }
            },
            sidebarExtras = emptyList()
        )
    )
}

class LootTableStudioConceptRenderer : BaseStudioConceptRenderer("loot_table") {
    @Composable
    override fun Render(context: StudioContext) {
        LootTableLayout(context)
    }
}
