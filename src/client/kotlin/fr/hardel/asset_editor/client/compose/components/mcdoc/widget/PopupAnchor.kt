package fr.hardel.asset_editor.client.compose.components.mcdoc.widget

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

class AnchorBelowPopup(private val gapPx: Int) : PopupPositionProvider {
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
