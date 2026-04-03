package fr.hardel.asset_editor.client.compose.routes.enchantment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.BreakpointRule
import fr.hardel.asset_editor.client.compose.components.ui.Card
import fr.hardel.asset_editor.client.compose.components.ui.LayoutSpec
import fr.hardel.asset_editor.client.compose.components.ui.ResponsiveGrid
import fr.hardel.asset_editor.client.compose.components.ui.Section
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.SlotConfigs
import fr.hardel.asset_editor.client.compose.StudioBreakpoint
import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

@Composable
fun EnchantmentSlotsPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, Registries.ENCHANTMENT) ?: return

    val activeSlots = remember(entry) {
        entry.data().definition().slots().map { it.getSerializedName() }.toSet()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Section(I18n.get("enchantment:section.slots.description")) {
            SlotConfigs.GROUPS.forEach { group ->
                ResponsiveGrid(
                    items = group.map { slotId ->
                        {
                            Card(
                                imageId = SlotConfigs.slotImage(slotId),
                                title = I18n.get("slot:$slotId"),
                                active = activeSlots.any { slot -> SlotConfigs.expandsTo(slot, slotId) },
                                onActiveChange = {
                                    context.dispatchRegistryAction(
                                        registry = Registries.ENCHANTMENT,
                                        target = entry.id(),
                                        action = EnchantmentEditorActions.ToggleSlot(slotId),
                                        dialogs = dialogs
                                    )
                                }
                            )
                        }
                    },
                    defaultSpec = LayoutSpec.AutoFit(256.dp),
                    rules = listOf(BreakpointRule(maxWidth = StudioBreakpoint.XL.px.dp, spec = LayoutSpec.Fixed(floatArrayOf(1f))))
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = I18n.get("enchantment:slots.explanation.title"),
                    style = VoxelTypography.regular(13),
                    color = VoxelColors.Zinc300
                )
                Text(
                    text = "- ${I18n.get("enchantment:slots.explanation.list.1")}",
                    style = VoxelTypography.light(13),
                    color = VoxelColors.Zinc400
                )
                Text(
                    text = "- ${I18n.get("enchantment:slots.explanation.list.2")}",
                    style = VoxelTypography.light(13),
                    color = VoxelColors.Zinc400
                )
            }
        }
    }

    RegistryPageDialogs(context, dialogs)
}
