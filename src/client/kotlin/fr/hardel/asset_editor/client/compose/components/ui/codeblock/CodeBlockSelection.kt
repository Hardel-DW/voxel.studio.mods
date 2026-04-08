package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

internal const val MULTI_TAP_TIMEOUT_MS = 350L
internal const val MULTI_TAP_DISTANCE_THRESHOLD_PX_SQ = 100f

internal class TapState(
    var count: Int = 0,
    var lastTime: Long = 0L,
    var lastPos: Offset = Offset.Zero
)

/**
 * Picks the natural selection range for a click at `offset` based on tap count:
 * 1 = caret, 2 = word, 3 = line.
 */
internal fun selectionForTapCount(text: String, offset: Int, tapCount: Int): TextRange =
    when (tapCount) {
        2 -> wordRangeAt(text, offset)
        3 -> lineRangeAt(text, offset)
        else -> TextRange(offset, offset)
    }

private fun wordRangeAt(text: String, offset: Int): TextRange {
    if (text.isEmpty()) return TextRange.Zero
    val safeOffset = offset.coerceIn(0, text.length)
    val pos = if (safeOffset >= text.length) safeOffset - 1 else safeOffset
    if (pos < 0) return TextRange(0, 0)

    val char = text[pos]
    return when {
        isWordChar(char) -> {
            var start = pos
            while (start > 0 && isWordChar(text[start - 1])) start--
            var end = pos + 1
            while (end < text.length && isWordChar(text[end])) end++
            TextRange(start, end)
        }
        char.isWhitespace() && char != '\n' -> {
            var start = pos
            while (start > 0 && text[start - 1].isWhitespace() && text[start - 1] != '\n') start--
            var end = pos + 1
            while (end < text.length && text[end].isWhitespace() && text[end] != '\n') end++
            TextRange(start, end)
        }
        else -> TextRange(pos, pos + 1)
    }
}

private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

private fun lineRangeAt(text: String, offset: Int): TextRange {
    if (text.isEmpty()) return TextRange.Zero
    val safeOffset = offset.coerceIn(0, text.length)

    var start = safeOffset
    while (start > 0 && text[start - 1] != '\n') start--

    var end = safeOffset
    while (end < text.length && text[end] != '\n') end++
    if (end < text.length) end++

    return TextRange(start, end)
}

internal fun handleKeyEvent(
    event: KeyEvent,
    text: String,
    selection: TextRange,
    setSelection: (TextRange) -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when {
        event.isCtrlPressed && event.key == Key.C -> {
            if (!selection.collapsed) {
                val s = minOf(selection.start, selection.end).coerceIn(0, text.length)
                val e = maxOf(selection.start, selection.end).coerceIn(0, text.length)
                SystemClipboard.setText(text.substring(s, e))
            }
            true
        }
        event.isCtrlPressed && event.key == Key.A -> {
            setSelection(TextRange(0, text.length))
            true
        }
        else -> false
    }
}

/**
 * Draws the selection rectangles for `selection` on a layout that contains a slice
 * of the text starting at `chunkStart`. Selection ends are intersected with the
 * chunk so that callers can blindly hand the same global selection to every chunk.
 */
internal fun DrawScope.drawSelectionRectangles(
    layout: TextLayoutResult,
    selection: TextRange,
    chunkStart: Int,
    chunkLength: Int,
    paddingLeftPx: Float,
    paddingTopPx: Float
) {
    if (selection.collapsed) return
    val globalStart = minOf(selection.start, selection.end)
    val globalEnd = maxOf(selection.start, selection.end)
    val s = (globalStart - chunkStart).coerceIn(0, chunkLength)
    val e = (globalEnd - chunkStart).coerceIn(0, chunkLength)
    if (e <= s) return

    val firstLine = layout.getLineForOffset(s)
    val lastLine = layout.getLineForOffset(e)

    for (line in firstLine..lastLine) {
        val top = paddingTopPx + layout.getLineTop(line)
        val bottom = paddingTopPx + layout.getLineBottom(line)

        val left = if (line == firstLine) {
            paddingLeftPx + layout.getHorizontalPosition(s, usePrimaryDirection = true)
        } else {
            paddingLeftPx + layout.getLineLeft(line)
        }

        val right = if (line == lastLine) {
            paddingLeftPx + layout.getHorizontalPosition(e, usePrimaryDirection = true)
        } else {
            paddingLeftPx + layout.getLineRight(line)
        }

        if (right > left) {
            drawRect(
                color = CODE_SELECTION_BG,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top)
            )
        }
    }
}
