package fr.hardel.asset_editor.client.compose.routes.enchantment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.layout.editor.HeaderActionButton
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.EnchantingSetupCard
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.EnchantingTableCard
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.EnchantmentResultsTable
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.EnchantmentSimulationState
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.MojangEnchantmentSimulator
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.SimulationWelcomeDialog
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.rememberEnchantmentSimulationState
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.FloatingBarState
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ItemSelector
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolGrab
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.Toolbar
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarNavigation
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarSize
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import net.minecraft.client.resources.language.I18n

@Composable
fun EnchantmentSimulationPage(@Suppress("UNUSED_PARAMETER") context: StudioContext) {
    val state = rememberEnchantmentSimulationState()
    val floatingBar = remember { FloatingBarState() }
    var welcomeVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item("header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = I18n.get("enchantment:simulation.title"),
                            style = StudioTypography.semiBold(24),
                            color = StudioColors.Zinc100
                        )
                        Text(
                            text = I18n.get("enchantment:simulation.description"),
                            style = StudioTypography.regular(12),
                            color = StudioColors.Zinc400
                        )
                    }
                    HeaderActionButton(
                        text = I18n.get("enchantment:simulation.toolbar.help"),
                        onClick = { welcomeVisible = true }
                    )
                }
            }
            item("cards") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(horizontal = 32.dp)
                ) {
                    EnchantingTableCard(
                        state = state,
                        onPickItem = { openItemPicker(floatingBar, state) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    EnchantingSetupCard(
                        state = state,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            item("results") {
                EnchantmentResultsTable(
                    stats = state.stats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                )
            }
            item("spacer") {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        Toolbar(floatingBar = floatingBar) {
            ToolGrab()
            ToolbarNavigation()
        }
    }

    if (welcomeVisible) {
        SimulationWelcomeDialog(onDismiss = { welcomeVisible = false })
    }
}

private fun openItemPicker(floatingBar: FloatingBarState, state: EnchantmentSimulationState) {
    val items = MojangEnchantmentSimulator.availableItems(state.mode)
    floatingBar.expand(size = ToolbarSize.LARGE) {
        ItemSelector(
            currentItem = state.itemId.toString(),
            items = items,
            onItemSelect = { id ->
                state.updateItem(id)
                floatingBar.collapse()
            },
            onCancel = { floatingBar.collapse() }
        )
    }
}
