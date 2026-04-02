package fr.hardel.asset_editor.client.compose.components.page.enchantment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.ToggleGroup
import fr.hardel.asset_editor.client.compose.components.ui.ToggleOption
import fr.hardel.asset_editor.client.compose.components.ui.tree.buildConceptTreeState
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.layout.editor.HeaderActionButton
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.navigation.ConceptSimulationDestination
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcepts
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.compose.lib.data.BaseStudioConceptRenderer
import fr.hardel.asset_editor.client.compose.lib.data.StudioEditorTabPages
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentOverviewPage
import kotlinx.collections.immutable.toImmutableSet
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries

@Composable
fun EnchantmentLayout(context: StudioContext, modifier: Modifier = Modifier) {
    val concept = StudioConcepts.requireByRegistryKey(Registries.ENCHANTMENT)
    val conceptUi = rememberConceptUi(context, concept)
    val entries = rememberRegistryEntries(context, Registries.ENCHANTMENT)
    val sidebarView = conceptUi.sidebarView
    val tree = remember(entries, sidebarView) { EnchantmentTreeBuilder.build(entries, sidebarView) }
    val folderIcons = remember(sidebarView) {
        when (sidebarView) {
            StudioSidebarView.SLOTS -> EnchantmentTreeBuilder.slotFolderIcons()
            StudioSidebarView.ITEMS -> EnchantmentTreeBuilder.itemFolderIcons()
            StudioSidebarView.EXCLUSIVE -> emptyMap()
        }
    }

    val currentEditor = rememberCurrentElementDestination(context, concept)
    val treeState = remember(
        tree,
        folderIcons,
        conceptUi.filterPath,
        conceptUi.expandedTreePaths,
        currentEditor?.elementId,
        sidebarView
    ) {
        buildConceptTreeState(
            concept = concept,
            tree = tree,
            filterPath = conceptUi.filterPath,
            selectedElementId = currentEditor?.elementId,
            expandedTreePaths = conceptUi.expandedTreePaths.toImmutableSet(),
            elementIcon = concept.icon,
            folderIcons = folderIcons,
            disableAutoExpand = sidebarView == StudioSidebarView.SLOTS,
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
    }

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            concept = concept,
            icon = concept.icon,
            sidebarTitleKey = "enchantment:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                when (destination) {
                    is ConceptOverviewDestination -> EnchantmentOverviewPage(context)
                    is ConceptSimulationDestination -> EmptyPage()
                    is ElementEditorDestination -> if (!StudioEditorTabPages.Render(context, destination)) EmptyPage()

                    else -> EmptyPage()
                }
            },
            headerActions = {
                HeaderActionButton(
                    text = I18n.get("enchantment:simulation"),
                    onClick = {
                        context.navigationMemory().navigate(
                            ConceptSimulationDestination(concept)
                        )
                    }
                )
            },
            sidebarExtras = listOf({
                ToggleGroup(
                    options = listOf(
                        ToggleOption.TextOption("slots", I18n.get("enchantment:overview.sidebar.slots")),
                        ToggleOption.TextOption("items", I18n.get("enchantment:overview.sidebar.items")),
                        ToggleOption.TextOption("exclusive", I18n.get("enchantment:overview.sidebar.exclusive"))
                    ),
                    selectedValue = conceptUi.sidebarView.name.lowercase(),
                    onValueChange = { value ->
                        context.uiMemory().updateSidebarView(
                            concept,
                            when (value) {
                                "items" -> StudioSidebarView.ITEMS
                                "exclusive" -> StudioSidebarView.EXCLUSIVE
                                else -> StudioSidebarView.SLOTS
                            }
                        )
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )
            })
        ),
        modifier = modifier
    )
}

class EnchantmentStudioConceptRenderer : BaseStudioConceptRenderer(
    conceptPath = "enchantment",
    supportsSimulation = true,
    shouldPrefetchAtlas = true
) {
    @Composable
    override fun Render(context: StudioContext) {
        EnchantmentLayout(context)
    }
}
