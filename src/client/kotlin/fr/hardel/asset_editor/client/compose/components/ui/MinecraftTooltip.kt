package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay

private const val PIXEL_SCALE = 3
private const val TOOLTIP_PADDING = 3
private const val TOOLTIP_MARGIN = 9
private const val TOOLTIP_OUTER = (TOOLTIP_PADDING + TOOLTIP_MARGIN) * PIXEL_SCALE
private const val CURSOR_OFFSET = 12

private val DEFAULT_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "tooltip/background")
private val DEFAULT_FRAME = Identifier.fromNamespaceAndPath("minecraft", "tooltip/frame")

data class TooltipLine(val text: String, val color: Color)

data class ItemTooltipData(
    val name: String,
    val nameColor: Color = StudioColors.TooltipName,
    val lines: List<TooltipLine> = emptyList(),
    val style: Identifier? = null,
    val hidden: Boolean = false
)

fun resolveItemTooltip(itemId: Identifier): ItemTooltipData {
    val minecraft = Minecraft.getInstance()
    val registryAccess = minecraft.connection?.registryAccess()
        ?: return ItemTooltipData(name = StudioTranslation.resolve("item", itemId))

    val key = ResourceKey.create(Registries.ITEM, itemId)
    val holder = registryAccess.lookupOrThrow(Registries.ITEM).get(key).orElse(null)
        ?: return ItemTooltipData(name = StudioTranslation.resolve("item", itemId))

    val stack = ItemStack(holder.value())
    val name = StudioTranslation.resolve("item", itemId)
    val style = stack.get(DataComponents.TOOLTIP_STYLE)

    val context = Item.TooltipContext.of(minecraft.level)
    val components = try {
        val display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
        if (display.hideTooltip()) return ItemTooltipData(name = "", hidden = true)
        stack.getTooltipLines(context, minecraft.player, TooltipFlag.ADVANCED)
    } catch (_: Exception) {
        null
    }

    if (components.isNullOrEmpty()) return ItemTooltipData(name = name, style = style)

    val nameComponent = components.first()
    return ItemTooltipData(
        name = nameComponent.string,
        nameColor = extractColor(nameComponent),
        lines = components.drop(1).map { TooltipLine(it.string, extractColor(it)) },
        style = style
    )
}

private fun extractColor(component: Component): Color {
    val textColor = component.style.color ?: return StudioColors.TooltipName
    return Color(textColor.value or (0xFF shl 24))
}

@Composable
fun MinecraftTooltip(data: ItemTooltipData, modifier: Modifier = Modifier) {
    val background = rememberNineSliceSprite(backgroundSpriteId(data.style))
    val frame = rememberNineSliceSprite(frameSpriteId(data.style))

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .drawBehind {
                val targetSize = IntSize(size.width.toInt(), size.height.toInt())
                background?.let { drawNineSlice(it, targetSize, PIXEL_SCALE) }
                frame?.let { drawNineSlice(it, targetSize, PIXEL_SCALE) }
            }
            .padding(TOOLTIP_OUTER.dp)
    ) {
        Text(text = data.name, style = StudioTypography.seven(16), color = data.nameColor)
        for (line in data.lines) {
            if (line.text.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Text(text = line.text, style = StudioTypography.seven(16), color = line.color)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MinecraftTooltipArea(
    itemId: Identifier,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    var cursorPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { event ->
                hovered = true
                cursorPosition = event.changes.firstOrNull()?.position ?: Offset.Zero
            }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .onPointerEvent(PointerEventType.Move) { event ->
                cursorPosition = event.changes.firstOrNull()?.position ?: Offset.Zero
            }
    ) {
        content()

        if (hovered) {
            val cursorPx = IntOffset(cursorPosition.x.toInt(), cursorPosition.y.toInt())
            Popup(popupPositionProvider = CursorTooltipPositionProvider(cursorPx)) {
                val data = remember(itemId) { resolveItemTooltip(itemId) }
                if (!data.hidden) {
                    MinecraftTooltip(data = data)
                }
            }
        }
    }
}

private class CursorTooltipPositionProvider(
    private val cursorLocal: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val cursorX = anchorBounds.left + cursorLocal.x
        val cursorY = anchorBounds.top + cursorLocal.y

        var x = cursorX + CURSOR_OFFSET
        var y = cursorY - CURSOR_OFFSET

        if (x + popupContentSize.width > windowSize.width) {
            x = cursorX - popupContentSize.width - CURSOR_OFFSET
        }
        if (y + popupContentSize.height > windowSize.height) {
            y = windowSize.height - popupContentSize.height
        }

        x = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        y = y.coerceAtLeast(0)

        return IntOffset(x, y)
    }
}

internal class AnchorTooltipPositionProvider(private val gapPx: Int) : PopupPositionProvider {
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
        return IntOffset(x, y.coerceAtLeast(0))
    }
}

private fun backgroundSpriteId(style: Identifier?): Identifier =
    style?.withPath { "tooltip/${it}_background" } ?: DEFAULT_BACKGROUND

private fun frameSpriteId(style: Identifier?): Identifier =
    style?.withPath { "tooltip/${it}_frame" } ?: DEFAULT_FRAME
