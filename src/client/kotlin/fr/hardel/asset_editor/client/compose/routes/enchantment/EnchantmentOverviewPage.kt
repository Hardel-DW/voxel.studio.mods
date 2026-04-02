package fr.hardel.asset_editor.client.compose.routes.enchantment

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ContentRow
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.ToggleSwitch
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.RegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcepts
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.lib.data.EnchantmentViewMatchers
import fr.hardel.asset_editor.store.ElementEntry
import fr.hardel.asset_editor.store.adapter.EnchantmentFlushAdapter
import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions
import java.util.Locale
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.enchantment.Enchantment

private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")

@Composable
fun EnchantmentOverviewPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entries = rememberRegistryEntries(context, Registries.ENCHANTMENT)
    val concept = StudioConcepts.requireByRegistryKey(Registries.ENCHANTMENT)
    val conceptUi = rememberConceptUi(context, concept)
    val search = conceptUi.search.trim().lowercase(Locale.ROOT)
    val filterPath = conceptUi.filterPath.trim().lowercase(Locale.ROOT)
    val sidebarView = conceptUi.sidebarView
    val filtered = remember(entries, search, filterPath, sidebarView) {
        entries
            .filter { entry -> search.isEmpty() || entry.id().path.contains(search) }
            .filter { entry -> EnchantmentViewMatchers.matches(entry, filterPath, sidebarView) }
            .sortedBy { entry -> entry.data().description().string }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            InputText(
                value = conceptUi.search,
                onValueChange = { value -> context.uiMemory().updateSearch(concept, value) },
                placeholder = I18n.get("enchantment:overview.search"),
                maxWidth = 576.dp
            )
        }

        if (filtered.isEmpty()) {
            EmptyOverviewState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                items(items = filtered, key = { entry -> entry.id().toString() }) { entry ->
                    OverviewRow(context, entry, dialogs)
                }
            }
        }
    }

    RegistryPageDialogs(context, dialogs)
}

@Composable
private fun EmptyOverviewState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .background(VoxelColors.Zinc900.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
            ) {
                SvgIcon(SEARCH_ICON, 40.dp, Color.White.copy(alpha = 0.2f))
            }
            Text(
                text = I18n.get("enchantment:items.no_results.title"),
                style = VoxelTypography.medium(20),
                color = VoxelColors.Zinc300
            )
            Text(
                text = I18n.get("enchantment:items.no_results.description"),
                style = VoxelTypography.regular(14),
                color = VoxelColors.Zinc500
            )
        }
    }
}

@Composable
private fun OverviewRow(
    context: StudioContext,
    entry: ElementEntry<Enchantment>,
    dialogs: RegistryDialogState
) {
    val concept = StudioConcepts.requireByRegistryKey(Registries.ENCHANTMENT)
    val enabled = !EnchantmentFlushAdapter.isSoftDeleted(entry)
    val itemId = remember(entry) {
        EnchantmentFlushAdapter.previewItemId(entry.data()) { tag -> context.resolveTag(Registries.ITEM, tag) }
    }
    val interaction = remember(entry.id()) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val openEntry = {
        context.navigationMemory().openElement(
            concept.editor(entry.id().toString(), concept.defaultEditorTab)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hovered) VoxelColors.Zinc900.copy(alpha = 0.6f) else VoxelColors.Zinc950.copy(alpha = 0.3f))
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = VoxelColors.Zinc800.copy(alpha = 0.3f),
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke
                )
            }
    ) {
        ContentRow(
            modifier = Modifier.hoverable(interaction),
            onClick = { openEntry() },
            onAction = { openEntry() },
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (itemId != null) {
                        ItemSprite(itemId, 32.dp)
                    } else {
                        Text("?", style = VoxelTypography.semiBold(10), color = VoxelColors.Zinc500)
                    }
                }
            },
            toggle = {
                ToggleSwitch(
                    checked = enabled,
                    onCheckedChange = {
                        context.dispatchRegistryAction(
                            registry = Registries.ENCHANTMENT,
                            target = entry.id(),
                            action = EnchantmentEditorActions.ToggleDisabled(),
                            dialogs = dialogs
                        )
                    }
                )
            }
        ) {
            Text(
                text = entry.data().description().string,
                style = VoxelTypography.medium(14),
                color = VoxelColors.Zinc200
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = entry.id().toString(),
                    style = VoxelTypography.regular(10).copy(fontFamily = FontFamily.Monospace),
                    color = VoxelColors.Zinc500
                )
                Text(
                    text = "\u2022",
                    style = VoxelTypography.regular(10),
                    color = VoxelColors.Zinc600
                )
                Text(
                    text = "${I18n.get("enchantment:overview.level")} ${entry.data().getMaxLevel()}",
                    style = VoxelTypography.regular(10),
                    color = VoxelColors.Zinc500
                )
            }
        }
    }
}
