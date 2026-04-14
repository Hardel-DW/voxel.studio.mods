package fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation

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
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.RomanNumerals.toRoman
import net.minecraft.resources.Identifier
import java.util.Locale

@Composable
fun EnchantmentItemTooltip(
    itemName: String,
    enchantments: List<SimulationEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(StudioColors.TooltipBackground)
            .border(1.dp, StudioColors.TooltipBorder)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = itemName,
            style = StudioTypography.seven(16),
            color = StudioColors.TooltipName
        )
        for (entry in enchantments) {
            Text(
                text = "${humanizeEnchantment(entry.enchantmentId)} ${toRoman(entry.level)}",
                style = StudioTypography.seven(16),
                color = StudioColors.TooltipEnchant
            )
        }
    }
}

private fun humanizeEnchantment(id: Identifier): String {
    val leaf = id.path.substringAfterLast('/')
    return leaf.split('_').joinToString(" ") { part ->
        if (part.isEmpty()) part else part.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}

internal object RomanNumerals {
    private val PAIRS = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )

    fun toRoman(value: Int): String {
        if (value <= 0) return value.toString()
        val builder = StringBuilder()
        var remaining = value
        for ((number, symbol) in PAIRS) {
            while (remaining >= number) {
                builder.append(symbol)
                remaining -= number
            }
        }
        return builder.toString()
    }
}
