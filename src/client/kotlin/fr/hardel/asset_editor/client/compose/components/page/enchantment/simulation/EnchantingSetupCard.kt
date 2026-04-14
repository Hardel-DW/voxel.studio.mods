package fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Counter
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SimpleCard
import fr.hardel.asset_editor.client.compose.components.ui.ToggleSwitch
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val ENCHANTING_TABLE_BLOCK = Identifier.fromNamespaceAndPath("minecraft", "textures/studio/block/enchanting_table.png")

@Composable
fun EnchantingSetupCard(
    state: EnchantmentSimulationState,
    modifier: Modifier = Modifier
) {
    SimpleCard(
        modifier = modifier,
        padding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            BookshelfPreviewColumn(
                bookshelves = state.bookshelves,
                onBookshelvesChange = state::updateBookshelves
            )
            VerticalDivider()
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp, alignment = Alignment.CenterVertically),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                SetupRow(
                    title = I18n.get("enchantment:simulation.enchantability.title"),
                    description = I18n.get("enchantment:simulation.enchantability.description")
                ) {
                    Counter(
                        value = state.enchantability,
                        min = 1,
                        max = 80,
                        step = 1,
                        onValueChange = state::updateEnchantability
                    )
                }
                SetupRow(
                    title = I18n.get("enchantment:simulation.mode.title"),
                    description = I18n.get("enchantment:simulation.mode.description")
                ) {
                    ToggleSwitch(
                        checked = state.mode == SimulationMode.WORKSPACE_ONLY,
                        onCheckedChange = { workspaceOnly ->
                            state.updateMode(if (workspaceOnly) SimulationMode.WORKSPACE_ONLY else SimulationMode.ALL_REGISTRIES)
                        }
                    )
                }
                SetupRow(
                    title = I18n.get("enchantment:simulation.tooltip.title"),
                    description = I18n.get("enchantment:simulation.tooltip.description")
                ) {
                    ToggleSwitch(
                        checked = state.showTooltip,
                        onCheckedChange = state::updateTooltip
                    )
                }
            }
        }
    }
}

@Composable
private fun BookshelfPreviewColumn(
    bookshelves: Int,
    onBookshelvesChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            ResourceImageIcon(location = ENCHANTING_TABLE_BLOCK, size = 96.dp)
        }
        Counter(
            value = bookshelves,
            min = 0,
            max = 15,
            step = 1,
            onValueChange = onBookshelvesChange
        )
    }
}

@Composable
private fun SetupRow(
    title: String,
    description: String,
    control: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f).padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = StudioTypography.medium(15),
                color = StudioColors.Zinc200
            )
            Text(
                text = description,
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500
            )
        }
        control()
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(2.dp)
            .fillMaxHeight()
            .background(StudioColors.Zinc900)
    )
}
