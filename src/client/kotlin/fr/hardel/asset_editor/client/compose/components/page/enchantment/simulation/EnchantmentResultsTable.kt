package fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.RomanNumerals.toRoman
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import kotlinx.collections.immutable.ImmutableList
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import java.util.Locale

private val EMPTY_BOOK_ITEM = Identifier.fromNamespaceAndPath("minecraft", "enchanted_book")

@Composable
fun EnchantmentResultsTable(
    stats: ImmutableList<SimulationStats>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ResultsHeader()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, StudioColors.Zinc900, RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Column {
                ResultsTableHeaderRow()
                if (stats.isEmpty()) {
                    EmptyResultsState()
                } else {
                    for ((index, stat) in stats.withIndex()) {
                        ResultsRow(stat = stat, alternate = index % 2 == 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = I18n.get("enchantment:simulation.results.title"),
            style = StudioTypography.semiBold(22),
            color = StudioColors.Zinc100
        )
        Text(
            text = I18n.get("enchantment:simulation.results.description.1"),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc400
        )
        Text(
            text = I18n.get("enchantment:simulation.results.description.2"),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc400
        )
    }
}

@Composable
private fun ResultsTableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeaderCell(I18n.get("enchantment:simulation.results.table.enchantment"), weight = 3f)
        TableHeaderCell(I18n.get("enchantment:simulation.results.table.probability"), weight = 1f)
        TableHeaderCell(I18n.get("enchantment:simulation.results.table.average_level"), weight = 1f)
        TableHeaderCell(I18n.get("enchantment:simulation.results.table.level_range"), weight = 1f)
    }
}

@Composable
private fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = StudioTypography.semiBold(13),
        color = StudioColors.Zinc200,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun ResultsRow(stat: SimulationStats, alternate: Boolean) {
    val background = if (alternate) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(3f)) {
            Text(
                text = humanizeIdentifier(stat.enchantmentId),
                style = StudioTypography.medium(14),
                color = Color.White
            )
            Text(
                text = stat.enchantmentId.toString(),
                style = StudioTypography.regular(10).copy(fontFamily = FontFamily.Monospace),
                color = StudioColors.Zinc500
            )
        }
        Text(
            text = "%.2f%%".format(Locale.ROOT, stat.probability),
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc300,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "%.1f".format(Locale.ROOT, stat.averageLevel),
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc300,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatLevelRange(stat.minLevel, stat.maxLevel),
            style = StudioTypography.medium(13),
            color = StudioColors.Zinc300,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyResultsState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(vertical = 56.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(StudioColors.Zinc900.copy(alpha = 0.5f))
                .border(1.dp, StudioColors.Zinc800, CircleShape)
        ) {
            ItemSprite(itemId = EMPTY_BOOK_ITEM, displaySize = 40.dp)
        }
        Text(
            text = I18n.get("enchantment:simulation.results.empty.title"),
            style = StudioTypography.medium(17),
            color = StudioColors.Zinc300
        )
        Text(
            text = I18n.get("enchantment:simulation.results.empty.description"),
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc500
        )
    }
}

private fun formatLevelRange(min: Int, max: Int): String {
    if (min == max) return toRoman(min)
    val separator = I18n.get("enchantment:simulation.results.level_range.to")
    return "${toRoman(min)} $separator ${toRoman(max)}"
}

private fun humanizeIdentifier(id: Identifier): String {
    val leaf = id.path.substringAfterLast('/')
    return leaf.split('_').joinToString(" ") { part ->
        if (part.isEmpty()) part else part.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
