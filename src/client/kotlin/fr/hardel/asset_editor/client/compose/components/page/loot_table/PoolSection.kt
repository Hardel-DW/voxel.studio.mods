package fr.hardel.asset_editor.client.compose.components.page.loot_table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import net.minecraft.client.resources.language.I18n
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import java.util.Locale

@Composable
fun PoolSection(
    pool: LootPool,
    poolIndex: Int,
    onAddItem: () -> Unit,
    onBalanceWeights: () -> Unit,
    onWeightChange: (entryIndex: Int, weight: Int) -> Unit,
    onDeleteEntry: (entryIndex: Int) -> Unit
) {
    val entries = pool.entries
    val totalWeight = entries.sumOf { entryWeight(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioColors.Zinc900.copy(alpha = 0.3f))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(12.dp))
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioColors.Zinc900.copy(alpha = 0.5f))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = I18n.get("loot:pools.pool_title", poolIndex + 1),
                    style = StudioTypography.semiBold(18),
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBadge(I18n.get("loot:pools.rolls").uppercase(Locale.ROOT), formatRolls(pool.rolls))
                    StatBadge(I18n.get("loot:pools.bonus_rolls").uppercase(Locale.ROOT), formatRolls(pool.bonusRolls))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    text = I18n.get("loot:pools.balance"),
                    variant = ButtonVariant.GHOST_BORDER,
                    size = ButtonSize.SM,
                    onClick = onBalanceWeights
                )
                Button(
                    text = I18n.get("loot:pools.add_item"),
                    variant = ButtonVariant.GHOST,
                    size = ButtonSize.SM,
                    onClick = onAddItem
                )
            }
        }

        Box(modifier = Modifier.padding(24.dp)) {
            if (entries.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                ) {
                    Text(
                        text = I18n.get("loot:pools.empty"),
                        style = StudioTypography.regular(14),
                        color = StudioColors.Zinc500
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (chunk in entries.withIndex().toList().chunked(4)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for ((idx, entry) in chunk) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PoolItemCard(
                                        entry = entry,
                                        totalWeight = totalWeight,
                                        onWeightChange = { w -> onWeightChange(idx, w) },
                                        onDelete = { onDeleteEntry(idx) }
                                    )
                                }
                            }
                            repeat(4 - chunk.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(StudioColors.Zinc800.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, style = StudioTypography.regular(12), color = StudioColors.Zinc500)
        Text(value, style = StudioTypography.medium(14), color = Color.White)
    }
}

private fun formatRolls(provider: NumberProvider): String = when (provider) {
    is ConstantValue -> provider.value().toInt().toString()
    is UniformGenerator -> {
        val min = (provider.min() as? ConstantValue)?.value()?.toInt()?.toString() ?: "?"
        val max = (provider.max() as? ConstantValue)?.value()?.toInt()?.toString() ?: "?"
        "$min-$max"
    }
    else -> "?"
}

private fun entryWeight(entry: LootPoolEntryContainer): Int =
    if (entry is LootPoolSingletonContainer) entry.weight else 1
