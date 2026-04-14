package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun MinecraftTooltip(
    name: String,
    modifier: Modifier = Modifier,
    enchantments: List<String> = emptyList(),
    lores: List<String> = emptyList(),
    attributes: List<String> = emptyList()
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(StudioColors.TooltipBackground)
            .border(1.dp, StudioColors.TooltipBorder)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = name,
            style = StudioTypography.seven(16),
            color = StudioColors.TooltipName
        )
        for (lore in lores) {
            Text(
                text = lore,
                style = StudioTypography.seven(16),
                color = StudioColors.TooltipLore
            )
        }
        for (enchantment in enchantments) {
            Text(
                text = enchantment,
                style = StudioTypography.seven(16),
                color = StudioColors.TooltipEnchant
            )
        }
        for (attribute in attributes) {
            Text(
                text = attribute,
                style = StudioTypography.seven(16),
                color = StudioColors.TooltipAttribute
            )
        }
    }
}
