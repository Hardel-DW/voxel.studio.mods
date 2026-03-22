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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.lib.data.EnchantmentViewMatchers
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.store.ElementEntry
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter
import fr.hardel.asset_editor.workspace.action.EditorAction
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
    val search = context.search.trim().lowercase(Locale.ROOT)
    val filterPath = context.filterPath.trim().lowercase(Locale.ROOT)

    val filtered = entries
        .filter { entry -> search.isEmpty() || entry.id().path.contains(search) }
        .filter { entry -> EnchantmentViewMatchers.matches(entry, filterPath, context.sidebarView) }
        .sortedBy { entry -> entry.data().description().string }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            InputText(
                value = context.search,
                onValueChange = { value -> context.uiState().setSearch(value) },
                placeholder = I18n.get("enchantment:overview.search")
            )
        }

        if (filtered.isEmpty()) {
            EmptyOverviewState()
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                filtered.forEach { entry ->
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
    val enabled = !EnchantmentFlushAdapter.isSoftDeleted(entry)
    val itemId = EnchantmentFlushAdapter.previewItemId(entry.data()) { tag -> context.resolveTag(Registries.ITEM, tag) }
    val interaction = remember(entry.id()) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val openEntry = {
        context.openTab(entry.id().toString(), StudioRoute.EnchantmentMain)
        context.router.navigate(StudioRoute.EnchantmentMain)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hovered) Color(0x9918181B) else Color(0x4D09090B))
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = Color(0x5927272A),
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
                            action = EditorAction.ToggleDisabled(),
                            dialogs = dialogs
                        )
                    }
                )
            }
        ) {
            Text(
                text = entry.data().description().string,
                style = VoxelTypography.medium(14),
                color = Color(0xFFE4E4E7)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = entry.id().toString(),
                    style = VoxelTypography.regular(10).copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF71717A)
                )
                Text(
                    text = "\u2022",
                    style = VoxelTypography.regular(10),
                    color = Color(0xFF5C5C66)
                )
                Text(
                    text = "${I18n.get("enchantment:overview.level")} ${entry.data().getMaxLevel()}",
                    style = VoxelTypography.regular(10),
                    color = Color(0xFF71717A)
                )
            }
        }
    }
}
