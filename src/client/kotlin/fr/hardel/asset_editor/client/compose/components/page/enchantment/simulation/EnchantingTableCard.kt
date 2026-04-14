package fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.MinecraftTooltip
import fr.hardel.asset_editor.client.compose.components.ui.SimpleCard
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

private val BOOK_OPEN = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/studio/gui/book_open.webp")
private val BOOK_CLOSED = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/studio/gui/book_closed.webp")

@Composable
fun EnchantingTableCard(
    state: EnchantmentSimulationState,
    onPickItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    SimpleCard(
        modifier = modifier,
        padding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            CardHeader()
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BookToggle(opened = state.showTooltip, onToggle = state::toggleTooltip)
                    SlotWithTooltip(state = state, onPickItem = onPickItem)
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    repeat(3) { index ->
                        val range = state.slotRanges.getOrNull(index)
                        SlotButton(
                            slotIndex = index,
                            minLevel = range?.minLevel ?: 0,
                            maxLevel = range?.maxLevel ?: 0,
                            selected = state.selectedSlot == index,
                            onClick = { state.runSimulation(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = I18n.get("enchantment:simulation.enchant_label"),
            style = StudioTypography.semiBold(18),
            color = StudioColors.Zinc100
        )
        Text(
            text = I18n.get("enchantment:simulation.enchant_sublabel"),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(1.dp)
                .background(StudioColors.Zinc800.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun BookToggle(opened: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val location = if (opened) BOOK_OPEN else BOOK_CLOSED
    val bitmap = LocalStudioAssetCache.current.bitmap(location) ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = Modifier
            .width(80.dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onToggle() }
    )
}

@Composable
private fun SlotWithTooltip(state: EnchantmentSimulationState, onPickItem: () -> Unit) {
    Box {
        EnchantingItemSlot(itemId = state.itemId, onClick = onPickItem)
        val option = state.currentOption
        if (state.showTooltip && option != null && option.entries.isNotEmpty()) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.BottomStart,
                offset = with(density) { IntOffset(16.dp.roundToPx(), 8.dp.roundToPx()) }
            ) {
                MinecraftTooltip(
                    name = StudioTranslation.resolve("item", state.itemId),
                    enchantments = option.entries.map { entry ->
                        val name = StudioTranslation.resolve(Registries.ENCHANTMENT, entry.enchantmentId)
                        "$name ${I18n.get("enchantment.level.${entry.level}")}"
                    }
                )
            }
        }
    }
}

@Composable
private fun EnchantingItemSlot(itemId: Identifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) StudioColors.Zinc800.copy(alpha = 0.6f) else StudioColors.Zinc900.copy(alpha = 0.5f))
            .border(2.dp, if (hovered) StudioColors.Zinc600 else StudioColors.Zinc800, RoundedCornerShape(8.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
    ) {
        ItemSprite(itemId = itemId, displaySize = 40.dp)
    }
}

@Composable
private fun SlotButton(
    slotIndex: Int,
    minLevel: Int,
    maxLevel: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val borderColor = when {
        selected -> Color(0xFF7A009F)
        hovered -> Color(0xFF52006F)
        else -> StudioColors.Zinc800
    }
    val background = when {
        selected -> Color(0xFF1A0026).copy(alpha = 0.45f)
        hovered -> Color(0xFF120018).copy(alpha = 0.4f)
        else -> StudioColors.Zinc900.copy(alpha = 0.5f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = I18n.get("enchantment:simulation.slot.${slotIndex + 1}"),
            style = StudioTypography.medium(13),
            color = StudioColors.Zinc300
        )
        Text(
            text = "$minLevel - $maxLevel",
            style = StudioTypography.seven(14),
            color = StudioColors.Experience
        )
    }
}
