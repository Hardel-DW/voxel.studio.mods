package fr.hardel.asset_editor.client.compose.routes.enchantment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.layout.SupportCard
import fr.hardel.asset_editor.client.compose.components.ui.BreakpointRule
import fr.hardel.asset_editor.client.compose.components.ui.Counter
import fr.hardel.asset_editor.client.compose.components.ui.LayoutSpec
import fr.hardel.asset_editor.client.compose.components.ui.ResponsiveGrid
import fr.hardel.asset_editor.client.compose.components.ui.Section
import fr.hardel.asset_editor.client.compose.components.ui.Selector
import fr.hardel.asset_editor.client.compose.components.ui.TemplateCard
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.data.StudioBreakpoint
import fr.hardel.asset_editor.store.adapter.EnchantmentFlushAdapter
import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

private val MAX_LEVEL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/max_level.svg")
private val WEIGHT_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/weight.svg")
private val ANVIL_COST_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/anvil_cost.svg")

@Composable
fun EnchantmentMainPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, Registries.ENCHANTMENT) ?: return

    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Section(I18n.get("enchantment:section.global.description")) {
            ResponsiveGrid(
                items = listOf(
                    {
                        TemplateCard(
                            iconPath = MAX_LEVEL_ICON,
                            title = I18n.get("enchantment:global.maxLevel.title"),
                            description = I18n.get("enchantment:global.explanation.list.1")
                        ) {
                            Counter(
                                value = entry.data().getMaxLevel(),
                                onValueChange = { value ->
                                    context.dispatchRegistryAction(
                                        registry = Registries.ENCHANTMENT,
                                        target = entry.id(),
                                        action = EnchantmentEditorActions.SetIntField("max_level", value),
                                        dialogs = dialogs
                                    )
                                },
                                min = 1,
                                max = 127,
                                step = 1
                            )
                        }
                    },
                    {
                        TemplateCard(
                            iconPath = WEIGHT_ICON,
                            title = I18n.get("enchantment:global.weight.title"),
                            description = I18n.get("enchantment:global.explanation.list.2")
                        ) {
                            Counter(
                                value = entry.data().getWeight(),
                                onValueChange = { value ->
                                    context.dispatchRegistryAction(
                                        registry = Registries.ENCHANTMENT,
                                        target = entry.id(),
                                        action = EnchantmentEditorActions.SetIntField("weight", value),
                                        dialogs = dialogs
                                    )
                                },
                                min = 1,
                                max = 1024,
                                step = 1
                            )
                        }
                    },
                    {
                        TemplateCard(
                            iconPath = ANVIL_COST_ICON,
                            title = I18n.get("enchantment:global.anvilCost.title"),
                            description = I18n.get("enchantment:global.explanation.list.3")
                        ) {
                            Counter(
                                value = entry.data().getAnvilCost(),
                                onValueChange = { value ->
                                    context.dispatchRegistryAction(
                                        registry = Registries.ENCHANTMENT,
                                        target = entry.id(),
                                        action = EnchantmentEditorActions.SetIntField("anvil_cost", value),
                                        dialogs = dialogs
                                    )
                                },
                                min = 0,
                                max = 255,
                                step = 1
                            )
                        }
                    }
                ),
                defaultSpec = LayoutSpec.AutoFit(256.dp),
                rules = listOf(BreakpointRule(maxWidth = StudioBreakpoint.XL.px.dp, spec = LayoutSpec.Fixed(floatArrayOf(1f))))
            )

            Selector(
                title = I18n.get("enchantment:global.mode.title"),
                description = I18n.get("enchantment:global.mode.description"),
                options = linkedMapOf(
                    EnchantmentFlushAdapter.MODE_NORMAL to I18n.get("enchantment:global.mode.enum.normal"),
                    EnchantmentFlushAdapter.MODE_DISABLE to I18n.get("enchantment:global.mode.enum.soft_delete"),
                    EnchantmentFlushAdapter.MODE_ONLY_CREATIVE to I18n.get("enchantment:global.mode.enum.only_creative")
                ),
                selectedValue = EnchantmentFlushAdapter.mode(entry),
                onValueChange = { value ->
                    context.dispatchRegistryAction(
                        registry = Registries.ENCHANTMENT,
                        target = entry.id(),
                        action = EnchantmentEditorActions.SetMode(value),
                        dialogs = dialogs
                    )
                }
            )
        }

        Spacer(Modifier.weight(1f))
        SupportCard()
    }

    RegistryPageDialogs(context, dialogs)
}
