package fr.hardel.asset_editor.client.compose.components.page.loot_table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.Counter
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.FloatingBarState
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ItemSelector
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarSize
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import java.util.Locale

private val PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")
private val TRASH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")

// Port 1:1 de decompiled/studio/src/components/tools/concept/loot/LootItemEditor.tsx
@Composable
fun LootItemEditor(
    reward: FlattenedReward,
    floatingBar: FloatingBarState,
    onWeightChange: (Int) -> Unit,
    onItemChange: (Identifier) -> Unit,
    onCountMinChange: (Int) -> Unit,
    onCountMaxChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val displayName = remember(reward) { rewardDisplayName(reward) }
    var weight by remember(reward.weight) { mutableIntStateOf(reward.weight) }
    var countMin by remember(reward.countMin) { mutableIntStateOf(reward.countMin) }
    var countMax by remember(reward.countMax) { mutableIntStateOf(reward.countMax) }
    var selectingItem by remember { mutableStateOf(false) }

    if (selectingItem) {
        ItemSelector(
            currentItem = reward.name.toString(),
            onItemSelect = {
                onItemChange(it)
                selectingItem = false
                floatingBar.resize(ToolbarSize.FIT)
            },
            onCancel = {
                selectingItem = false
                floatingBar.resize(ToolbarSize.FIT)
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header — flex items-center justify-between pb-4 border-b border-zinc-800/50
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp) // gap-3
            ) {
                // Pencil icon — size-8 rounded-lg bg-zinc-800/50
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp) // size-8
                        .clip(RoundedCornerShape(8.dp)) // rounded-lg
                        .background(StudioColors.Zinc800.copy(alpha = 0.5f))
                ) {
                    SvgIcon(PENCIL_ICON, 16.dp, Color.White.copy(alpha = 0.6f)) // size-4 invert opacity-60
                }
                Column {
                    Text(
                        text = I18n.get("loot:editor.title"),
                        style = StudioTypography.semiBold(14), // text-sm font-semibold
                        color = StudioColors.Zinc100 // text-zinc-100
                    )
                    Text(
                        text = displayName,
                        style = StudioTypography.regular(12), // text-xs
                        color = StudioColors.Zinc500 // text-zinc-500
                    )
                }
            }

            // Delete button — size-9 hover:text-red-400 hover:bg-red-500/10 rounded-lg
            DeleteButton(onClick = {
                onDelete()
                onClose()
            })
        }

        // Divider
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioColors.Zinc800.copy(alpha = 0.5f)))

        // Body — flex-1 py-4
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp), // gap-6
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp) // py-4
        ) {
            // Item change button — size-28 (112dp), rounded-xl, border-zinc-800
            ChangeItemButton(reward.name) {
                floatingBar.resize(ToolbarSize.LARGE)
                selectingItem = true
            }

            // Right side: Weight + Count
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp), // gap-5
                modifier = Modifier.weight(1f)
            ) {
                // Weight row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = I18n.get("loot:editor.weight_label"),
                            style = StudioTypography.medium(14), // text-sm font-medium text-zinc-200
                            color = StudioColors.Zinc200
                        )
                        Text(
                            text = I18n.get("loot:editor.weight_description"),
                            style = StudioTypography.regular(12), // text-xs text-zinc-500
                            color = StudioColors.Zinc500
                        )
                    }
                    Counter(
                        value = weight,
                        min = 1,
                        max = 999,
                        step = 1,
                        onValueChange = { newWeight ->
                            weight = newWeight
                            onWeightChange(newWeight)
                        }
                    )
                }

                // Divider — h-px bg-zinc-800/50
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioColors.Zinc800.copy(alpha = 0.5f)))

                // Count row (read-only for now)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = I18n.get("loot:editor.count_label"),
                            style = StudioTypography.medium(14),
                            color = StudioColors.Zinc200
                        )
                        Text(
                            text = I18n.get("loot:editor.count_description"),
                            style = StudioTypography.regular(12),
                            color = StudioColors.Zinc500
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // gap-2
                    ) {
                        Counter(
                            value = countMin,
                            min = 1,
                            max = countMax,
                            step = 1,
                            onValueChange = { newMin ->
                                countMin = newMin
                                onCountMinChange(newMin)
                            }
                        )
                        Text("\u2014", style = StudioTypography.regular(14), color = StudioColors.Zinc600) // —
                        Counter(
                            value = countMax,
                            min = countMin,
                            max = 64,
                            step = 1,
                            onValueChange = { newMax ->
                                countMax = newMax
                                onCountMaxChange(newMax)
                            }
                        )
                    }
                }
            }
        }

        // Footer divider
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioColors.Zinc800.copy(alpha = 0.5f)))

        // Footer — pt-4 flex justify-between items-center
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp) // pt-4
        ) {
            Text(
                text = I18n.get("loot:editor.footer_text"),
                style = StudioTypography.medium(12), // text-xs font-medium
                color = StudioColors.Zinc400 // text-zinc-400
            )
            Button(
                text = I18n.get("loot:editor.close"),
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                onClick = onClose
            )
        }
    }
}

@Composable
private fun ChangeItemButton(itemId: Identifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically), // gap-1
        modifier = Modifier
            .size(112.dp) // size-28
            .clip(RoundedCornerShape(12.dp)) // rounded-xl
            .background(
                Brush.verticalGradient(
                    if (hovered) listOf(StudioColors.Zinc700.copy(alpha = 0.3f), StudioColors.Zinc800.copy(alpha = 0.5f))
                    else listOf(StudioColors.Zinc800.copy(alpha = 0.3f), StudioColors.Zinc900.copy(alpha = 0.5f))
                )
            )
            .border(
                1.dp,
                if (hovered) StudioColors.Zinc600 else StudioColors.Zinc800,
                RoundedCornerShape(12.dp)
            )
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Purple glow on hover — bg-purple-500/10 blur-xl
            if (hovered) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { alpha = 0.6f }
                        .background(StudioColors.Violet500.copy(alpha = 0.1f), RoundedCornerShape(50))
                )
            }
            ItemSprite(itemId, 48.dp)
        }
        Text(
            text = I18n.get("loot:editor.change_item"),
            style = StudioTypography.medium(12),
            color = if (hovered) StudioColors.Zinc300 else StudioColors.Zinc500
        )
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp) // size-9
            .clip(RoundedCornerShape(8.dp)) // rounded-lg
            .background(if (hovered) StudioColors.Red500.copy(alpha = 0.1f) else Color.Transparent)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        SvgIcon(
            TRASH_ICON, 16.dp, // size-4
            if (hovered) StudioColors.Red400 else StudioColors.Zinc500
        )
    }
}

internal fun rewardDisplayName(reward: FlattenedReward): String {
    val domain = when (reward.kind) {
        RewardKind.TAG -> "item_tag"
        RewardKind.LOOT_TABLE -> "loot_table"
        else -> "item"
    }
    return StudioTranslation.resolve(domain, reward.name)
}
