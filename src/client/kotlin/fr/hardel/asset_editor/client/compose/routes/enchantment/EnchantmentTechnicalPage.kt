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
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.*
import fr.hardel.asset_editor.client.compose.lib.*
import fr.hardel.asset_editor.workspace.flush.adapter.EnchantmentFlushAdapter
import fr.hardel.asset_editor.workspace.flush.Workspaces
import fr.hardel.asset_editor.workspace.action.EditorAction
import fr.hardel.asset_editor.workspace.action.enchantment.SetIntFieldAction
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleDisabledEffectAction
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleTagAction
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.resources.Identifier

private data class BehaviourTag(val tagId: Identifier) {
    fun titleKey(): String = "enchantment_tag:$tagId"
    fun descKey(): String = "enchantment_tag:$tagId.desc"
}

private data class CostField(val key: String, val value: (Enchantment) -> Int, val action: (Int) -> EditorAction<*>)

private val BEHAVIOUR_TAGS = listOf(
    BehaviourTag(EnchantmentFlushAdapter.SMELTS_LOOT_TAG),
    BehaviourTag(EnchantmentFlushAdapter.PREVENTS_ICE_MELTING_TAG),
    BehaviourTag(EnchantmentFlushAdapter.PREVENTS_INFESTED_SPAWNS_TAG),
    BehaviourTag(EnchantmentFlushAdapter.PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG),
    BehaviourTag(EnchantmentFlushAdapter.PREVENTS_DECORATED_POT_SHATTERING_TAG),
    BehaviourTag(EnchantmentFlushAdapter.DOUBLE_TRADE_PRICE_TAG)
)

private val COST_FIELDS = listOf(
    CostField(
        "minCostBase",
        { enchantment -> enchantment.definition().minCost().base() },
        { value -> SetIntFieldAction("min_cost_base", value) }),
    CostField(
        "minCostPerLevelAboveFirst",
        { enchantment -> enchantment.definition().minCost().perLevelAboveFirst() },
        { value -> SetIntFieldAction("min_cost_per_level", value) }),
    CostField(
        "maxCostBase",
        { enchantment -> enchantment.definition().maxCost().base() },
        { value -> SetIntFieldAction("max_cost_base", value) }),
    CostField(
        "maxCostPerLevelAboveFirst",
        { enchantment -> enchantment.definition().maxCost().perLevelAboveFirst() },
        { value -> SetIntFieldAction("max_cost_per_level", value) })
)

@Composable
fun EnchantmentTechnicalPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, Registries.ENCHANTMENT) ?: return
    val effects = remember(entry) { EnchantmentFlushAdapter.availableEffects(entry.data()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Section(I18n.get("enchantment:section.technical.description")) {
            ResponsiveGrid(
                items = BEHAVIOUR_TAGS.map { field ->
                    {
                        SwitchCard(
                            title = I18n.get(field.titleKey()),
                            description = I18n.get(field.descKey()),
                            checked = entry.tags().contains(field.tagId),
                            onCheckedChange = {
                                context.dispatchRegistryAction(
                                    definition = Workspaces.ENCHANTMENT,
                                    target = entry.id(),
                                    action = ToggleTagAction(field.tagId),
                                    dialogs = dialogs
                                )
                            }
                        )
                    }
                },
                defaultSpec = LayoutSpec.Fixed(floatArrayOf(1f)),
                rules = listOf(
                    BreakpointRule(
                        minWidth = StudioBreakpoint.LG.px.dp,
                        spec = LayoutSpec.Fixed(floatArrayOf(1f, 1f))
                    )
                )
            )
        }

        Section(I18n.get("enchantment:section.costs")) {
            ResponsiveGrid(
                items = COST_FIELDS.map { field ->
                    {
                        Range(
                            label = I18n.get("enchantment:global.${field.key}.title"),
                            value = field.value(entry.data()),
                            onValueChange = { value ->
                                context.dispatchRegistryAction(
                                    definition = Workspaces.ENCHANTMENT,
                                    target = entry.id(),
                                    action = field.action(value),
                                    dialogs = dialogs
                                )
                            },
                            min = 0,
                            max = 100,
                            step = 1
                        )
                    }
                },
                defaultSpec = LayoutSpec.Fixed(floatArrayOf(1f)),
                rules = listOf(
                    BreakpointRule(
                        minWidth = StudioBreakpoint.LG.px.dp,
                        spec = LayoutSpec.Fixed(floatArrayOf(1f, 1f))
                    )
                )
            )
        }

        Section(I18n.get("enchantment:technical.effects.title")) {
            if (effects.isEmpty()) {
                Text(
                    text = I18n.get("enchantment:technical.empty_effects"),
                    style = VoxelTypography.regular(14),
                    color = VoxelColors.Zinc400,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                ResponsiveGrid(
                    items = effects.map { effectId ->
                        {
                            val effectKey = Identifier.tryParse(effectId)
                            val effectLabel =
                                if (effectKey != null) StudioText.resolve("effect", effectKey) else effectId
                            val descKey = if (effectKey != null) "effect:$effectKey.desc" else ""
                            SwitchCard(
                                title = effectLabel,
                                description = if (descKey.isNotBlank() && I18n.exists(descKey)) I18n.get(descKey) else effectId,
                                checked = !EnchantmentFlushAdapter.isEffectDisabled(entry, effectId),
                                onCheckedChange = {
                                    context.dispatchRegistryAction(
                                        definition = Workspaces.ENCHANTMENT,
                                        target = entry.id(),
                                        action = ToggleDisabledEffectAction(effectId),
                                        dialogs = dialogs
                                    )
                                }
                            )
                        }
                    },
                    defaultSpec = LayoutSpec.Fixed(floatArrayOf(1f)),
                    rules = listOf(
                        BreakpointRule(
                            minWidth = StudioBreakpoint.LG.px.dp,
                            spec = LayoutSpec.Fixed(floatArrayOf(1f, 1f))
                        )
                    )
                )
            }
        }
    }

    RegistryPageDialogs(context, dialogs)
}
