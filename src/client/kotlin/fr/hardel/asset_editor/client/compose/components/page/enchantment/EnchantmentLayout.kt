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
import fr.hardel.asset_editor.client.compose.components.ui.LoadingPlaceholder
import fr.hardel.asset_editor.client.compose.components.layout.editor.HeaderActionButton
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry
import fr.hardel.asset_editor.client.compose.lib.ConceptSimulationDestination
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberModifiedIds
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioDestination
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentOverviewPage
import fr.hardel.asset_editor.client.compose.routes.enchantment.EnchantmentSimulationPage
import kotlinx.collections.immutable.toImmutableMap
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries

@Composable
fun EnchantmentLayout(context: StudioContext, modifier: Modifier = Modifier) {
    val conceptId = context.studioConceptId(Registries.ENCHANTMENT) ?: return
    val compendiumItems = rememberServerData(StudioDataSlots.COMPENDIUM_ITEMS)
    val dataReady = compendiumItems.isNotEmpty()
    val conceptUi = rememberConceptUi(context, conceptId)
    val entries = rememberRegistryEntries(context, ClientWorkspaceRegistries.ENCHANTMENT)
    val modifiedIds = rememberModifiedIds(ClientWorkspaceRegistries.ENCHANTMENT)
    val sidebarView = conceptUi.sidebarView
    val filteredEntries = remember(entries, conceptUi.showAll, modifiedIds) {
        if (conceptUi.showAll) entries else entries.filter { it.id() in modifiedIds }
    }
    val tree = remember(filteredEntries, sidebarView, compendiumItems) { EnchantmentTreeBuilder.build(filteredEntries, sidebarView) }
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
        conceptUi.treeExpansion,
        currentEditor?.elementId,
        sidebarView,
        conceptUi.showAll,
        modifiedIds
    ) {
        buildConceptTreeState(
            conceptId = conceptId,
            tree = tree,
            filterPath = conceptUi.filterPath,
            selectedElementId = currentEditor?.elementId,
            treeExpansion = conceptUi.treeExpansion.toImmutableMap(),
            elementIcon = conceptIcon,
            folderIcons = folderIcons,
            disableAutoExpand = sidebarView == StudioSidebarView.SLOTS,
            totalCount = entries.size,
            modifiedCount = modifiedIds.size,
            showAll = conceptUi.showAll,
            onSelectAll = {
                context.uiMemory().setShowAll(conceptId, true)
                context.navigationMemory().navigate(ConceptOverviewDestination(conceptId))
            },
            onSelectChanges = { context.uiMemory().setShowAll(conceptId, false) },
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

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            conceptId = conceptId,
            registryKey = Registries.ENCHANTMENT,
            icon = conceptIcon,
            sidebarTitleKey = "enchantment:overview.title",
            treeState = treeState,
            pageFactory = { destination: StudioDestination ->
                if (!dataReady) {
                    LoadingPlaceholder(I18n.get("studio:loading.server_data"))
                } else when (destination) {
                    is ConceptOverviewDestination -> EnchantmentOverviewPage(context)
                    is ConceptSimulationDestination -> EnchantmentSimulationPage(context)
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
