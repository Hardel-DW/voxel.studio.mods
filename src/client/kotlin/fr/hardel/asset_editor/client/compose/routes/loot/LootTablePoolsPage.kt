package fr.hardel.asset_editor.client.compose.routes.loot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.loot_table.PoolSection
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.FloatingBarState
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ItemSelector
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolGrab
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.Toolbar
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarNavigation
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarSize
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.workspace.action.loot_table.AddEntryAction
import fr.hardel.asset_editor.workspace.action.loot_table.BalancePoolWeightsAction
import fr.hardel.asset_editor.workspace.action.loot_table.EntryPath
import fr.hardel.asset_editor.workspace.action.loot_table.RemoveEntryAction
import fr.hardel.asset_editor.workspace.action.loot_table.SetEntryWeightAction
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun LootTablePoolsPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, ClientWorkspaceRegistries.LOOT_TABLE) ?: return
    val table = entry.data()
    val pools = table.pools
    val floatingBar = remember { FloatingBarState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioColors.Zinc950.copy(alpha = 0.75f))
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = I18n.get("loot:pools.title"),
                    style = StudioTypography.bold(24),
                    color = Color.White
                )
                Button(
                    text = I18n.get("loot:pools.add_pool"),
                    variant = ButtonVariant.DEFAULT,
                    onClick = {
                        context.dispatchRegistryAction(
                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                            target = entry.id(),
                            action = AddEntryAction(pools.size, Identifier.withDefaultNamespace("stone"), 1),
                            dialogs = dialogs
                        )
                    }
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                if (pools.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                    ) {
                        Text(
                            text = I18n.get("loot:main.empty"),
                            style = StudioTypography.medium(14),
                            color = StudioColors.Zinc400
                        )
                    }
                }

                for ((poolIndex, pool) in pools.withIndex()) {
                    PoolSection(
                        pool = pool,
                        poolIndex = poolIndex,
                        onAddItem = {
                            val targetPool = poolIndex
                            floatingBar.expand(size = ToolbarSize.LARGE) {
                                ItemSelector(
                                    onItemSelect = { itemId ->
                                        context.dispatchRegistryAction(
                                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                            target = entry.id(),
                                            action = AddEntryAction(targetPool, itemId, 1),
                                            dialogs = dialogs
                                        )
                                        floatingBar.collapse()
                                    },
                                    onCancel = { floatingBar.collapse() }
                                )
                            }
                        },
                        onBalanceWeights = {
                            context.dispatchRegistryAction(
                                workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                target = entry.id(),
                                action = BalancePoolWeightsAction(poolIndex),
                                dialogs = dialogs
                            )
                        },
                        onWeightChange = { entryIndex, weight ->
                            context.dispatchRegistryAction(
                                workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                target = entry.id(),
                                action = SetEntryWeightAction(EntryPath.ofTopLevel(poolIndex, entryIndex), weight),
                                dialogs = dialogs
                            )
                        },
                        onDeleteEntry = { entryIndex ->
                            context.dispatchRegistryAction(
                                workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                target = entry.id(),
                                action = RemoveEntryAction(EntryPath.ofTopLevel(poolIndex, entryIndex)),
                                dialogs = dialogs
                            )
                        }
                    )
                }
            }
        }

        Toolbar(floatingBar = floatingBar) {
            ToolGrab()
            ToolbarNavigation()
        }
    }

    RegistryPageDialogs(context, dialogs)
}
