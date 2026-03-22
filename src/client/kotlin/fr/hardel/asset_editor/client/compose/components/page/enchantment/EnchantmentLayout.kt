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
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.navigation.StudioEditorTab
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentExclusivePage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentFindPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentItemsPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentMainPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentOverviewPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentSlotsPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentTechnicalPage
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries

@Composable
fun EnchantmentLayout(context: StudioContext, modifier: Modifier = Modifier) {
    val concept = StudioConcept.ENCHANTMENT
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
            expandedTreePaths = conceptUi.expandedTreePaths,
            elementIcon = concept.icon,
            folderIcons = folderIcons,
            disableAutoExpand = sidebarView == StudioSidebarView.SLOTS,
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
    }

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            concept = concept,
            icon = concept.icon,
            sidebarTitleKey = "enchantment:overview.title",
            treeState = treeState,
            simulationTab = StudioEditorTab.SIMULATION,
            showViewModeToggle = true,
            pageFactory = { destination: StudioDestination ->
                when (destination) {
                    is ConceptOverviewDestination -> EnchantmentOverviewPage(context)
                    is ElementEditorDestination -> when (destination.tab) {
                        StudioEditorTab.MAIN -> EnchantmentMainPage(context)
                        StudioEditorTab.FIND -> EnchantmentFindPage(context)
                        StudioEditorTab.SLOTS -> EnchantmentSlotsPage(context)
                        StudioEditorTab.ITEMS -> EnchantmentItemsPage(context)
                        StudioEditorTab.EXCLUSIVE -> EnchantmentExclusivePage(context)
                        StudioEditorTab.TECHNICAL -> EnchantmentTechnicalPage(context)
                        StudioEditorTab.SIMULATION -> EmptyPage()
                        else -> EmptyPage()
                    }

                    else -> EmptyPage()
                }
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
                        context.uiState().updateSidebarView(
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
