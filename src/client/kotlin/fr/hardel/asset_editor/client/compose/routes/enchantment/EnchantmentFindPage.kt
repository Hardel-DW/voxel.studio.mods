package fr.hardel.asset_editor.client.compose.routes.enchantment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
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
import fr.hardel.asset_editor.client.compose.lib.data.StudioBreakpoint
import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

private data class FindTag(val tagId: Identifier) {
    fun titleKey(): String = "enchantment_tag:$tagId"
    fun descKey(): String = "enchantment_tag:$tagId.desc"
    fun image(): Identifier = tagId.withPath("textures/studio/enchantment/${tagId.path}.png")
}

private val FIND_TAGS = listOf(
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "in_enchanting_table")),
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "on_mob_spawn_equipment")),
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "on_random_loot")),
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "tradeable")),
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "on_traded_equipment")),
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "curse")),
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "non_treasure")),
    FindTag(Identifier.fromNamespaceAndPath("minecraft", "treasure"))
)

@Composable
fun EnchantmentFindPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, Registries.ENCHANTMENT) ?: return

    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Section(I18n.get("enchantment:section.find")) {
            ResponsiveGrid(
                items = FIND_TAGS.map { tag ->
                    {
                        Card(
                            imageId = tag.image(),
                            title = I18n.get(tag.titleKey()),
                            description = I18n.get(tag.descKey()),
                            active = entry.tags().contains(tag.tagId),
                            onActiveChange = {
                                context.dispatchRegistryAction(
                                    registry = Registries.ENCHANTMENT,
                                    target = entry.id(),
                                    action = EnchantmentEditorActions.ToggleTag(tag.tagId),
                                    dialogs = dialogs
                                )
                            }
                        )
                    }
                },
                defaultSpec = LayoutSpec.AutoFit(368.dp),
                rules = listOf(BreakpointRule(maxWidth = StudioBreakpoint.XL.px.dp, spec = LayoutSpec.Fixed(floatArrayOf(1f))))
            )
        }
    }

    RegistryPageDialogs(context, dialogs)
}
