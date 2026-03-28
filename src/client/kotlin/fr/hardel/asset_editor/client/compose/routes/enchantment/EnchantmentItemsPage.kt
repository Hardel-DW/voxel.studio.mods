package fr.hardel.asset_editor.client.compose.routes.enchantment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.BreakpointRule
import fr.hardel.asset_editor.client.compose.components.ui.Card
import fr.hardel.asset_editor.client.compose.components.ui.LayoutSpec
import fr.hardel.asset_editor.client.compose.components.ui.ResponsiveGrid
import fr.hardel.asset_editor.client.compose.components.ui.SectionSelector
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.data.EnchantmentTreeData
import fr.hardel.asset_editor.client.compose.lib.data.StudioBreakpoint
import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

private val NONE_IMAGE = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/cross.png")

@Composable
fun EnchantmentItemsPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, Registries.ENCHANTMENT) ?: return
    var section by remember { mutableStateOf("supportedItems") }

    val supportedTag = remember(entry) {
        entry.data().definition().supportedItems().unwrapKey()
            .map { key -> key.location() }
            .orElse(null)
    }
    val primaryTag = remember(entry) {
        entry.data().definition().primaryItems()
            .flatMap { holderSet -> holderSet.unwrapKey() }
            .map { key -> key.location() }
            .orElse(null)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        SectionSelector(
            title = I18n.get("enchantment:section.supported.description"),
            tabs = linkedMapOf(
                "supportedItems" to I18n.get("enchantment:toggle.supported.title"),
                "primaryItems" to I18n.get("enchantment:toggle.primary.title")
            ),
            selectedTab = section,
            onTabChange = { value -> section = value }
        ) {
            ResponsiveGrid(
                items = buildList {
                    EnchantmentTreeData.ITEM_TAGS.forEach { tag ->
                        add {
                            val selected = if (section == "primaryItems") primaryTag == tag.tagId else supportedTag == tag.tagId
                            Card(
                                imageId = tag.icon(),
                                title = StudioText.resolve("item_tag", tag.tagId),
                                active = selected,
                                onActiveChange = {
                                    val action = if (section == "primaryItems") {
                                        EnchantmentEditorActions.SetPrimaryItems(tag.tagId.toString(), tag.seed)
                                    } else {
                                        EnchantmentEditorActions.SetSupportedItems(tag.tagId.toString(), tag.seed)
                                    }
                                    context.dispatchRegistryAction(
                                        registry = Registries.ENCHANTMENT,
                                        target = entry.id(),
                                        action = action,
                                        dialogs = dialogs
                                    )
                                }
                            )
                        }
                    }

                    if (section == "primaryItems") {
                        add {
                            Card(
                                imageId = NONE_IMAGE,
                                title = I18n.get("enchantment.supported:none"),
                                active = primaryTag == null,
                                onActiveChange = {
                                    context.dispatchRegistryAction(
                                        registry = Registries.ENCHANTMENT,
                                        target = entry.id(),
                                        action = EnchantmentEditorActions.SetPrimaryItems("", null),
                                        dialogs = dialogs
                                    )
                                }
                            )
                        }
                    }
                },
                defaultSpec = LayoutSpec.AutoFit(256.dp),
                rules = listOf(BreakpointRule(maxWidth = StudioBreakpoint.XL.px.dp, spec = LayoutSpec.Fixed(floatArrayOf(1f))))
            )
        }
    }

    RegistryPageDialogs(context, dialogs)
}
