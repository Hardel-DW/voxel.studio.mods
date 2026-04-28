package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import fr.hardel.asset_editor.client.compose.StudioText
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Key(
    label: String,
    modifier: Modifier = Modifier,
    doc: String? = null,
    raw: Boolean = false,
    deprecated: Boolean = false,
    color: Color = McdocTokens.Text,
    minWidth: Dp = McdocTokens.LabelMinWidth
) {
    val displayLabel = if (raw) label else StudioText.humanize(label)
    val baseStyle = StudioTypography.medium(12)
    val style = if (deprecated) baseStyle.copy(textDecoration = TextDecoration.LineThrough) else baseStyle
    val shape = RoundedCornerShape(topStart = McdocTokens.Radius, bottomStart = McdocTokens.Radius)
    var hovered by remember { mutableStateOf(false) }

    val labelModifier = modifier
        .height(McdocTokens.RowHeight)
        .widthIn(min = minWidth)
        .clip(shape)
        .background(McdocTokens.LabelBg, shape)
        .border(1.dp, McdocTokens.Border, shape)
        .padding(horizontal = McdocTokens.PaddingX)

    val finalModifier = if (doc != null) {
        labelModifier
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
    } else labelModifier

    Box(contentAlignment = Alignment.CenterStart, modifier = finalModifier) {
        val textModifier = if (doc != null) {
            Modifier.drawBehind {
                val strokeWidth = 1.dp.toPx()
                val dash = 2.dp.toPx()
                val gap = 2.dp.toPx()
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = if (hovered) McdocTokens.Text else McdocTokens.TextDimmed,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
                )
            }
        } else Modifier
        Text(
            text = displayLabel,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = textModifier
        )
        if (doc != null && hovered) {
            Popup(popupPositionProvider = AnchorBelow(6)) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .clip(RoundedCornerShape(McdocTokens.Radius))
                        .background(McdocTokens.TooltipBg, RoundedCornerShape(McdocTokens.Radius))
                        .border(1.dp, McdocTokens.TooltipBorder, RoundedCornerShape(McdocTokens.Radius))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(text = doc, style = StudioTypography.regular(12), color = McdocTokens.Text)
                }
            }
        }
    }
}

private class AnchorBelow(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val yBelow = anchorBounds.bottom + gapPx
        val yAbove = anchorBounds.top - popupContentSize.height - gapPx
        val y = if (yBelow + popupContentSize.height <= windowSize.height) yBelow else yAbove
        return IntOffset(x, y.coerceAtLeast(0))
    }
}
