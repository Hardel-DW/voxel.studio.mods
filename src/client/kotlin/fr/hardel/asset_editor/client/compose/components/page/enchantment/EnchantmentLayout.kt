package fr.hardel.asset_editor.client.compose.components.page.enchantment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayout
import fr.hardel.asset_editor.client.compose.components.layout.editor.ConceptLayoutConfig
import fr.hardel.asset_editor.client.compose.components.ui.ToggleGroup
import fr.hardel.asset_editor.client.compose.components.ui.ToggleOption
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeController
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.routes.EmptyPage
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
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
    val entries = rememberRegistryEntries(context, Registries.ENCHANTMENT)
    val sidebarView = context.sidebarView
    val tree = remember(entries, sidebarView) { EnchantmentTreeBuilder.build(entries, sidebarView) }
    val folderIcons = remember(sidebarView) {
        when (sidebarView) {
            StudioSidebarView.SLOTS -> EnchantmentTreeBuilder.slotFolderIcons()
            StudioSidebarView.ITEMS -> EnchantmentTreeBuilder.itemFolderIcons()
            StudioSidebarView.EXCLUSIVE -> emptyMap()
        }
    }

    ConceptLayout(
        context = context,
        config = ConceptLayoutConfig(
            concept = StudioConcept.ENCHANTMENT,
            icon = StudioConcept.ENCHANTMENT.icon,
            sidebarTitleKey = "enchantment:overview.title",
            treeConfig = TreeController.Config(
                overviewRoute = StudioRoute.EnchantmentOverview,
                detailRoute = StudioRoute.EnchantmentMain,
                changesRoute = StudioRoute.ChangesMain,
                concept = StudioConcept.ENCHANTMENT.registry(),
                tabRoutes = StudioConcept.ENCHANTMENT.tabRoutes(),
                tree = tree,
                elementIcon = StudioConcept.ENCHANTMENT.icon,
                folderIcons = folderIcons,
                disableAutoExpand = sidebarView == StudioSidebarView.SLOTS,
                filterPath = { context.filterPath },
                setFilterPath = { value -> context.uiState().setFilterPath(value) },
                currentElementId = { context.currentElementId },
                setCurrentElementId = { value -> context.tabsState().setCurrentElementId(value ?: "") },
                activeTabElementId = { context.currentElementId },
                onOpenElement = { elementId, _ ->
                    context.openTab(elementId, StudioRoute.EnchantmentMain)
                    context.router.navigate(StudioRoute.EnchantmentMain)
                }
            ),
            simulationRoute = StudioRoute.EnchantmentSimulation,
            showViewModeToggle = true,
            pageFactory = { route ->
                when (route) {
                    StudioRoute.EnchantmentOverview -> EnchantmentOverviewPage(context)
                    StudioRoute.EnchantmentMain -> EnchantmentMainPage(context)
                    StudioRoute.EnchantmentFind -> EnchantmentFindPage(context)
                    StudioRoute.EnchantmentSlots -> EnchantmentSlotsPage(context)
                    StudioRoute.EnchantmentItems -> EnchantmentItemsPage(context)
                    StudioRoute.EnchantmentExclusive -> EnchantmentExclusivePage(context)
                    StudioRoute.EnchantmentTechnical -> EnchantmentTechnicalPage(context)
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
                    selectedValue = context.sidebarView.name.lowercase(),
                    onValueChange = { value ->
                        context.updateSidebarView(
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
