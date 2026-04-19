package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.lib.assets.drawNineSlice
import fr.hardel.asset_editor.client.compose.lib.assets.rememberNineSliceSprite
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack

private const val MINECRAFT_PIXEL_SCALE = 3
private const val TOOLTIP_PADDING = 3
private const val TOOLTIP_MARGIN = 9
private const val TOOLTIP_OUTER = (TOOLTIP_PADDING + TOOLTIP_MARGIN) * MINECRAFT_PIXEL_SCALE
private const val TOOLTIP_GAP_DP = 8

private val DEFAULT_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "tooltip/background")
private val DEFAULT_FRAME = Identifier.fromNamespaceAndPath("minecraft", "tooltip/frame")

@Composable
fun MinecraftTooltip(
    name: String,
    modifier: Modifier = Modifier,
    enchantments: List<String> = emptyList(),
    lores: List<String> = emptyList(),
    attributes: List<String> = emptyList(),
    tooltipStyle: Identifier? = null
) {
    val background = rememberNineSliceSprite(backgroundSpriteId(tooltipStyle))
    val frame = rememberNineSliceSprite(frameSpriteId(tooltipStyle))

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .drawBehind {
                val size = IntSize(size.width.toInt(), size.height.toInt())
                background?.let { drawNineSlice(it, size, MINECRAFT_PIXEL_SCALE) }
                frame?.let { drawNineSlice(it, size, MINECRAFT_PIXEL_SCALE) }
            }
            .padding(horizontal = TOOLTIP_OUTER.dp, vertical = TOOLTIP_OUTER.dp)
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

/**
 * Wraps [content] and shows a [MinecraftTooltip] on hover, resolved from [itemId].
 * Position: below the anchor by default, above if no room, clamped to window bounds.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MinecraftTooltipArea(
    itemId: Identifier,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
    ) {
        content()

        if (hovered) {
            val gapPx = with(LocalDensity.current) { TOOLTIP_GAP_DP.dp.roundToPx() }
            Popup(popupPositionProvider = TooltipPositionProvider(gapPx)) {
                val data = remember(itemId) { resolveItemTooltip(itemId) }
                MinecraftTooltip(
                    name = data.name,
                    lores = data.lores,
                    enchantments = data.enchantments,
                    tooltipStyle = data.style
                )
            }
        }
    }
}

data class ItemTooltipData(
    val name: String,
    val lores: List<String> = emptyList(),
    val enchantments: List<String> = emptyList(),
    val style: Identifier? = null
)

fun resolveItemTooltip(itemId: Identifier): ItemTooltipData {
    val name = StudioTranslation.resolve("item", itemId)
    val registryAccess = Minecraft.getInstance().connection?.registryAccess()
        ?: return ItemTooltipData(name)
    val key = ResourceKey.create(Registries.ITEM, itemId)
    val holder = registryAccess.lookupOrThrow(Registries.ITEM).get(key).orElse(null)
        ?: return ItemTooltipData(name)
    val stack = ItemStack(holder.value())
    val style = stack.get(DataComponents.TOOLTIP_STYLE)
    val lore = stack.get(DataComponents.LORE)
        ?.lines()
        ?.map { it.string }
        ?: emptyList()
    return ItemTooltipData(name = name, lores = lore, style = style)
}

class TooltipPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val yBelow = anchorBounds.bottom + gapPx
        val yAbove = anchorBounds.top - popupContentSize.height - gapPx
        val y = if (yBelow + popupContentSize.height <= windowSize.height) yBelow else yAbove
        return IntOffset(x, y)
    }
}

private fun backgroundSpriteId(style: Identifier?): Identifier =
    style?.withPath { "tooltip/${it}_background" } ?: DEFAULT_BACKGROUND

private fun frameSpriteId(style: Identifier?): Identifier =
    style?.withPath { "tooltip/${it}_frame" } ?: DEFAULT_FRAME
