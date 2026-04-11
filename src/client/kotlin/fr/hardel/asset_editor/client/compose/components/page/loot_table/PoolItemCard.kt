package fr.hardel.asset_editor.client.compose.components.page.loot_table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.entries.NestedLootTable
import net.minecraft.world.level.storage.loot.entries.TagEntry
import java.util.Locale

@Composable
fun PoolItemCard(
    entry: LootPoolEntryContainer,
    totalWeight: Int,
    onWeightChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val weight = if (entry is LootPoolSingletonContainer) entry.weight else 1
    val chance = if (totalWeight > 0) "%.1f".format(weight.toDouble() / totalWeight * 100) else "0.0"
    val entryInfo = remember(entry) { extractEntryInfo(entry) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioColors.Zinc950.copy(alpha = 0.5f))
            .border(1.dp, StudioColors.Zinc900, RoundedCornerShape(12.dp))
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Transparent)
                    .border(1.dp, StudioColors.Zinc600.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = entryInfo.typeName.uppercase(Locale.ROOT),
                    style = StudioTypography.medium(10),
                    color = StudioColors.Zinc300
                )
            }

            Box(
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDelete() }
                    .padding(6.dp)
            ) {
                Text("\u2715", style = StudioTypography.regular(12), color = StudioColors.Zinc500)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp)
            ) {
                when {
                    entryInfo.itemId != null -> ItemSprite(entryInfo.itemId, 40.dp)
                    entryInfo.isTag -> Text("#", style = StudioTypography.bold(18), color = StudioColors.Zinc500)
                    entryInfo.isNested -> Text("\u21B3", style = StudioTypography.bold(18), color = StudioColors.Zinc500)
                    else -> Text("\u2205", style = StudioTypography.bold(18), color = StudioColors.Zinc500)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entryInfo.displayName,
                    style = StudioTypography.medium(14),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entryInfo.identifier,
                    style = StudioTypography.regular(12).copy(fontFamily = FontFamily.Monospace),
                    color = StudioColors.Zinc500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = I18n.get("loot:card.chance_label").uppercase(Locale.ROOT),
                    style = StudioTypography.regular(10),
                    color = StudioColors.Zinc500
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(chance, style = StudioTypography.bold(20), color = Color.White)
                    Text("%", style = StudioTypography.regular(14), color = StudioColors.Zinc400, modifier = Modifier.padding(start = 2.dp, bottom = 1.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = I18n.get("loot:card.weight_label").uppercase(Locale.ROOT),
                    style = StudioTypography.regular(10),
                    color = StudioColors.Zinc500
                )
                WeightStepper(weight, onWeightChange)
            }
        }
    }
}

@Composable
private fun WeightStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(StudioColors.Zinc900.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        StepperButton("-") { onChange(maxOf(1, value - 1)) }
        Text(
            text = value.toString(),
            style = StudioTypography.medium(14),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        StepperButton("+") { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = StudioTypography.regular(14), color = StudioColors.Zinc400)
    }
}

private data class EntryInfo(
    val typeName: String,
    val displayName: String,
    val identifier: String,
    val itemId: Identifier? = null,
    val isTag: Boolean = false,
    val isNested: Boolean = false
)

private fun extractEntryInfo(entry: LootPoolEntryContainer): EntryInfo {
    return when (entry) {
        is LootItem -> {
            val id = entry.item.unwrapKey().orElse(null)?.identifier()
            EntryInfo(
                typeName = "Item",
                displayName = humanize(id),
                identifier = id?.toString() ?: "unknown",
                itemId = id
            )
        }
        is TagEntry -> EntryInfo(
            typeName = "Tag",
            displayName = "#${entry.tag.location().path}",
            identifier = "#${entry.tag.location()}",
            isTag = true
        )
        is NestedLootTable -> {
            val ref = entry.contents.left().orElse(null)?.identifier()
            EntryInfo(
                typeName = "Loot Table",
                displayName = humanize(ref),
                identifier = ref?.toString() ?: "inline",
                isNested = true
            )
        }
        else -> EntryInfo(
            typeName = entry.type.toString(),
            displayName = "Unknown",
            identifier = ""
        )
    }
}

private fun humanize(id: Identifier?): String {
    if (id == null) return "Unknown"
    val leaf = id.path.substringAfterLast('/')
    return leaf.split('_').joinToString(" ") { part ->
        if (part.isEmpty()) part else part.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
