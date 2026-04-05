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
import fr.hardel.asset_editor.client.compose.lib.ConceptChangesDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry
import fr.hardel.asset_editor.client.compose.lib.ConceptSimulationDestination
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.session.server.StudioDataSlots
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioDestination
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentOverviewPage
import kotlinx.collections.immutable.toImmutableSet
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries

@Composable
fun EnchantmentLayout(context: StudioContext, modifier: Modifier = Modifier) {
    val conceptId = context.studioConceptId(Registries.ENCHANTMENT) ?: return
    val conceptUi = rememberConceptUi(context, conceptId)
    val entries = rememberRegistryEntries(context, Registries.ENCHANTMENT)
    val sidebarView = conceptUi.sidebarView
    val compendiumItems = rememberServerData(StudioDataSlots.COMPENDIUM_ITEMS)
    val tree = remember(entries, sidebarView, compendiumItems) { EnchantmentTreeBuilder.build(entries, sidebarView) }
    val folderIcons = remember(sidebarView) {
        when (sidebarView) {
            StudioSidebarView.SLOTS -> EnchantmentTreeBuilder.slotFolderIcons()
            StudioSidebarView.ITEMS -> EnchantmentTreeBuilder.itemFolderIcons()
            StudioSidebarView.EXCLUSIVE -> emptyMap()
        }
    }

    val currentEditor = rememberCurrentElementDestination(context, conceptId)
    val defaultEditorTab = remember(conceptId) { context.studioDefaultEditorTab(conceptId) }
    val conceptIcon = remember(conceptId) { context.studioIcon(conceptId) }
    val treeState = remember(
        tree,
        folderIcons,
        conceptUi.filterPath,
        conceptUi.expandedTreePaths,
        currentEditor?.elementId,
        sidebarView
    ) {
        buildConceptTreeState(
            conceptId = conceptId,
            tree = tree,
            filterPath = conceptUi.filterPath,
            selectedElementId = currentEditor?.elementId,
            expandedTreePaths = conceptUi.expandedTreePaths.toImmutableSet(),
            elementIcon = conceptIcon,
            folderIcons = folderIcons,
            disableAutoExpand = sidebarView == StudioSidebarView.SLOTS,
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
    }

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            conceptId = conceptId,
            registryKey = Registries.ENCHANTMENT,
            icon = conceptIcon,
            sidebarTitleKey = "enchantment:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                when (destination) {
                    is ConceptOverviewDestination -> EnchantmentOverviewPage(context)
                    is ConceptSimulationDestination -> EmptyPage()
                    is ElementEditorDestination -> if (!StudioUiRegistry.renderPage(context, destination)) EmptyPage()

                    else -> EmptyPage()
                }
            },
            headerActions = {
                HeaderActionButton(
                    text = I18n.get("enchantment:simulation"),
                    onClick = {
                        context.navigationMemory().navigate(
                            ConceptSimulationDestination(conceptId)
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
                            conceptId,
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
