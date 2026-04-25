package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.layout.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.ui.LoadingPlaceholder
import fr.hardel.asset_editor.client.compose.components.ui.ToggleGroup
import fr.hardel.asset_editor.client.compose.components.ui.ToggleOption
import fr.hardel.asset_editor.client.compose.components.ui.tree.ConceptTreeState
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.components.ui.tree.buildConceptTreeState
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioDestination
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.compose.routes.structure.StructureOverviewPage
import kotlinx.collections.immutable.toImmutableMap
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.core.Registry

val STRUCTURE_CONCEPT_ID: Identifier = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure")
val STRUCTURE_REGISTRY_ID: Identifier = Identifier.withDefaultNamespace("structure")
val STRUCTURE_REGISTRY_KEY: ResourceKey<Registry<Any>> = ResourceKey.createRegistryKey(STRUCTURE_REGISTRY_ID)

@Composable
fun StructureLayout(context: StudioContext) {
    val conceptId = STRUCTURE_CONCEPT_ID
    val templates = rememberServerData(StudioDataSlots.STRUCTURE_TEMPLATES)
    val worldgen = rememberServerData(StudioDataSlots.STRUCTURE_WORLDGEN)
    val conceptUi = rememberConceptUi(context, conceptId)
    val currentEditor = rememberCurrentElementDestination(context, conceptId)
    val defaultEditorTab = remember(conceptId) { context.studioDefaultEditorTab(conceptId) }
    val conceptIcon = remember(conceptId) { context.studioIcon(conceptId) }
    val viewMode = StructureUiState.viewMode

    val tree = remember(viewMode, templates, worldgen) {
        when (viewMode) {
            StructureViewMode.PIECES -> StructureTreeBuilder.build(templates)
            StructureViewMode.STRUCTURE -> StructureWorldgenTreeBuilder.build(worldgen, context.assetCache())
        }
    }
    val totalCount = if (viewMode == StructureViewMode.PIECES) templates.size else worldgen.size

    val treeState = buildTreeState(
        context = context,
        conceptId = conceptId,
        tree = tree,
        conceptIcon = conceptIcon,
        totalCount = totalCount,
        defaultEditorTab = defaultEditorTab,
        currentElementId = currentEditor?.elementId,
        filterPath = conceptUi.filterPath,
        treeExpansion = conceptUi.treeExpansion
    )

    val isLoading = when (viewMode) {
        StructureViewMode.PIECES -> templates.isEmpty()
        StructureViewMode.STRUCTURE -> worldgen.isEmpty()
    }

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            conceptId = conceptId,
            registryKey = STRUCTURE_REGISTRY_KEY,
            icon = conceptIcon,
            sidebarTitleKey = "structure:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                if (isLoading) {
                    LoadingPlaceholder(I18n.get("structure:loading"))
                } else when (destination) {
                    is ConceptOverviewDestination -> when (viewMode) {
                        StructureViewMode.PIECES -> StructureOverviewPage(context, templates)
                        StructureViewMode.STRUCTURE -> StructureWorldgenOverviewPage(context, worldgen)
                    }
                    is ElementEditorDestination -> if (!StudioUiRegistry.renderPage(context, destination)) EmptyPage()
                    else -> EmptyPage()
                }
            },
            sidebarExtras = listOf({
                ToggleGroup(
                    options = listOf(
                        ToggleOption.TextOption(StructureViewMode.PIECES.id, I18n.get("structure:sidebar.pieces")),
                        ToggleOption.TextOption(StructureViewMode.STRUCTURE.id, I18n.get("structure:sidebar.structure"))
                    ),
                    selectedValue = StructureUiState.viewMode.id,
                    onValueChange = { StructureUiState.viewMode = StructureViewMode.fromId(it) },
                    modifier = Modifier.padding(top = 16.dp)
                )
            })
        )
    )
}

@Composable
private fun buildTreeState(
    context: StudioContext,
    conceptId: Identifier,
    tree: TreeNodeModel,
    conceptIcon: Identifier,
    totalCount: Int,
    defaultEditorTab: Identifier,
    currentElementId: String?,
    filterPath: String,
    treeExpansion: Map<String, Boolean>
): ConceptTreeState = remember(tree, filterPath, treeExpansion, currentElementId) {
    buildConceptTreeState(
        conceptId = conceptId,
        tree = tree,
        filterPath = filterPath,
        selectedElementId = currentElementId,
        treeExpansion = treeExpansion.toImmutableMap(),
        elementIcon = conceptIcon,
        folderIcons = emptyMap(),
        disableAutoExpand = false,
        totalCount = totalCount,
        modifiedCount = 0,
        showAll = true,
        onSelectAll = {
            context.uiMemory().setShowAll(conceptId, true)
            context.navigationMemory().navigate(ConceptOverviewDestination(conceptId))
        },
        onSelectChanges = {},
        onSelectFolder = { path ->
            context.uiMemory().updateFilterPath(conceptId, path)
            context.navigationMemory().navigate(ConceptOverviewDestination(conceptId))
        },
        onSelectElement = { elementId ->
            context.navigationMemory().openElement(ElementEditorDestination(conceptId, elementId, defaultEditorTab))
        },
        onToggleExpanded = { path, expanded ->
            context.uiMemory().setTreeExpanded(conceptId, path, expanded)
        }
    )
}
