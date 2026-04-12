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
import fr.hardel.asset_editor.client.compose.StudioBreakpoint
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.*
import fr.hardel.asset_editor.client.compose.lib.*
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleSlotAction
import net.minecraft.client.resources.language.I18n

@Composable
fun EnchantmentSlotsPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, ClientWorkspaceRegistries.ENCHANTMENT) ?: return

    val activeSlots = remember(entry) {
        entry.data().definition().slots().map { it.serializedName }.toSet()
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
                                        workspace = ClientWorkspaceRegistries.ENCHANTMENT,
                                        target = entry.id(),
                                        action = ToggleSlotAction(slotId),
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
                    style = StudioTypography.regular(13),
                    color = StudioColors.Zinc300
                )
                Text(
                    text = "- ${I18n.get("enchantment:slots.explanation.list.1")}",
                    style = StudioTypography.light(13),
                    color = StudioColors.Zinc400
                )
                Text(
                    text = "- ${I18n.get("enchantment:slots.explanation.list.2")}",
                    style = StudioTypography.light(13),
                    color = StudioColors.Zinc400
                )
            }
        }
    }

    RegistryPageDialogs(context, dialogs)
}
