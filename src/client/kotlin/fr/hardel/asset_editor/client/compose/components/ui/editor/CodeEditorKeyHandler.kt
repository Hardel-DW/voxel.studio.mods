package fr.hardel.asset_editor.client.compose.components.ui.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * Translates a [KeyEvent] into a mutation on [state]. Returns `true` if the
 * event was handled (in which case the caller should consume it via
 * `onPreviewKeyEvent`).
 *
 * Modifier policy:
 * - Pure `Ctrl` and pure `Alt` shortcuts go through their dedicated branches.
 * - `Ctrl + Alt` is treated as **Alt-Gr** (the right-Alt convention used by
 *   Windows for special characters like `@`, `#`, `[`, `]` on AZERTY layouts)
 *   and is allowed to fall through to text input.
 * - Text input is rejected when exactly one of `Ctrl` / `Alt` is held — this
 *   is what stops Ctrl+letter / Alt+letter from inserting a stray `?`
 *   (replacement character) into the buffer.
 */
internal fun handleEditorKeyEvent(
    event: KeyEvent,
    state: CodeEditorState,
    clipboard: ClipboardManager,
    requestScrollToCaret: () -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.key in MODIFIER_KEYS) return false

    val shift = event.isShiftPressed
    val ctrl = event.isCtrlPressed
    val alt = event.isAltPressed
    val isAltGr = ctrl && alt

    if (ctrl && !isAltGr) {
        when (event.key) {
            Key.A -> { state.selectAll(); return true }
            Key.C -> { copy(state, clipboard); return true }
            Key.X -> { cut(state, clipboard); requestScrollToCaret(); return true }
            Key.V -> { paste(state, clipboard); requestScrollToCaret(); return true }
            Key.Z -> {
                if (shift) state.redo() else state.undo()
                requestScrollToCaret()
                return true
            }
            Key.Y -> { state.redo(); requestScrollToCaret(); return true }
            Key.Backspace -> { state.deleteWordBackward(); requestScrollToCaret(); return true }
            Key.Delete -> { state.deleteWordForward(); requestScrollToCaret(); return true }
            Key.Home -> { state.moveCaretToDocStart(shift); requestScrollToCaret(); return true }
            Key.MoveEnd -> { state.moveCaretToDocEnd(shift); requestScrollToCaret(); return true }
            Key.DirectionLeft -> { state.moveCaretByWord(forward = false, extend = shift); requestScrollToCaret(); return true }
            Key.DirectionRight -> { state.moveCaretByWord(forward = true, extend = shift); requestScrollToCaret(); return true }
        }
    }

    if (alt && !isAltGr) {
        when (event.key) {
            Key.DirectionUp -> { state.moveLine(MoveDirection.UP); requestScrollToCaret(); return true }
            Key.DirectionDown -> { state.moveLine(MoveDirection.DOWN); requestScrollToCaret(); return true }
        }
    }

    when (event.key) {
        Key.Backspace -> { state.deleteBackward(); requestScrollToCaret(); return true }
        Key.Delete -> { state.deleteForward(); requestScrollToCaret(); return true }
        Key.Enter, Key.NumPadEnter -> { state.insertNewlineWithIndent(); requestScrollToCaret(); return true }
        Key.Tab -> { state.insertTab(); requestScrollToCaret(); return true }
        Key.DirectionLeft -> { state.moveCaretBy(-1, shift); requestScrollToCaret(); return true }
        Key.DirectionRight -> { state.moveCaretBy(1, shift); requestScrollToCaret(); return true }
        Key.DirectionUp -> { state.moveCaretByLine(-1, shift); requestScrollToCaret(); return true }
        Key.DirectionDown -> { state.moveCaretByLine(1, shift); requestScrollToCaret(); return true }
        Key.Home -> { state.moveCaretToLineStart(shift); requestScrollToCaret(); return true }
        Key.MoveEnd -> { state.moveCaretToLineEnd(shift); requestScrollToCaret(); return true }
        Key.PageUp -> { state.moveCaretByLine(-PAGE_LINES, shift); requestScrollToCaret(); return true }
        Key.PageDown -> { state.moveCaretByLine(PAGE_LINES, shift); requestScrollToCaret(); return true }
    }

    val canInsertText = (!ctrl && !alt) || isAltGr
    if (canInsertText && event.utf16CodePoint.isPrintableText()) {
        state.insert(event.utf16CodePoint.toChar().toString())
        requestScrollToCaret()
        return true
    }

    return false
}

/**
 * True when the codepoint is a real printable character — excludes control
 * ranges, surrogate halves, the Unicode replacement character and the
 * CHAR_UNDEFINED sentinel (0xFFFF) that AWT emits for modifier-only key
 * presses on Compose Desktop.
 */
private fun Int.isPrintableText(): Boolean {
    if (this <= 0) return false
    if (this < 0x20 || this == 0x7F) return false
    if (this in 0x80..0x9F) return false
    if (this in 0xD800..0xDFFF) return false
    if (this == 0xFFFD || this == 0xFFFE || this == 0xFFFF) return false
    return true
}

private val MODIFIER_KEYS = setOf(
    Key.ShiftLeft, Key.ShiftRight,
    Key.CtrlLeft, Key.CtrlRight,
    Key.AltLeft, Key.AltRight,
    Key.MetaLeft, Key.MetaRight,
    Key.CapsLock, Key.NumLock, Key.ScrollLock,
    Key.Function
)

private fun copy(state: CodeEditorState, clipboard: ClipboardManager) {
    val text = if (state.selection.collapsed) state.fullText() else state.selectionText()
    if (text.isNotEmpty()) clipboard.setText(AnnotatedString(text))
}

private fun cut(state: CodeEditorState, clipboard: ClipboardManager) {
    if (state.selection.collapsed) return
    clipboard.setText(AnnotatedString(state.selectionText()))
    state.insert("")
}

private fun paste(state: CodeEditorState, clipboard: ClipboardManager) {
    val text = clipboard.getText()?.text ?: return
    if (text.isEmpty()) return
    state.insert(text)
}

/** Approx page size for PgUp / PgDn — keep simple, the editor isn't a terminal. */
private const val PAGE_LINES = 20
