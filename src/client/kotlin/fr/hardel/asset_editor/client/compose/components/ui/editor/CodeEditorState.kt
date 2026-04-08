package fr.hardel.asset_editor.client.compose.components.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CODE_TEXT_STYLE

/**
 * Compose-observable state for [CodeEditor]. Owns the [EditableLineBuffer],
 * the caret/selection, the per-line highlight cache and the undo stack, and
 * exposes high-level edit operations the key handler can call directly.
 *
 * Selection convention: [selection] holds the *anchor* in `start` and the
 * *active end* in `end`. When `start == end` the selection is collapsed and
 * the editor renders a caret at that offset. Edit operations always replace
 * the selected range, then move the caret just after whatever was inserted.
 *
 * Every successful edit bumps [documentVersion] so callers (the renderer,
 * the validation hook, …) can `remember(documentVersion) { … }` to react.
 */
@Stable
class CodeEditorState(initialText: String = "") {

    internal val buffer = EditableLineBuffer(initialText)
    private val undo = EditorUndoStack()
    private val highlighter = JsonLineHighlighter()
    internal val cache = LineHighlightCache(highlighter)

    var documentVersion by mutableLongStateOf(buffer.documentVersion)
        private set

    var selection by mutableStateOf(TextRange.Zero)

    var textStyle by mutableStateOf(
        CODE_TEXT_STYLE.merge(TextStyle(color = StudioColors.Zinc300, fontSize = 14.sp))
    )
    var backgroundFill by mutableStateOf(StudioColors.Zinc950)
    var borderFill by mutableStateOf(StudioColors.Zinc800)
    var minHeight by mutableStateOf(0.dp)

    /**
     * Unit used by [insertTab] and [insertNewlineWithIndent]. Defaults to four
     * spaces; set to `"  "` (two spaces) to match Gson-style JSON, or `"\t"`
     * for tab-indented files.
     */
    var indentUnit: String by mutableStateOf("    ")

    val length: Int get() = buffer.length
    val lineCount: Int get() = buffer.lineCount

    fun lineAt(index: Int): EditableLineBuffer.Line = buffer.lineAt(index)

    /** Builds the full text on demand (used by copy-all and external consumers). */
    fun fullText(): String = buffer.toText()

    /**
     * Replaces the current selection (if any) with [text] and moves the caret
     * just after the inserted content. Records both the implicit delete and
     * the insert in the undo stack so a single Ctrl+Z reverts both halves.
     */
    fun insert(text: String) {
        if (text.isEmpty() && selection.collapsed) return
        val s = minOf(selection.start, selection.end)
        val e = maxOf(selection.start, selection.end)
        if (s != e) {
            val removed = buffer.substring(s, e)
            buffer.delete(s, e)
            undo.record(EditOp.Delete(s, removed))
        }
        if (text.isNotEmpty()) {
            val end = buffer.insert(s, text)
            undo.record(EditOp.Insert(s, text))
            selection = TextRange(end)
        } else {
            selection = TextRange(s)
        }
        documentVersion = buffer.documentVersion
    }

    fun deleteBackward() {
        if (!selection.collapsed) {
            insert("")
            return
        }
        val pos = selection.start
        if (pos == 0) return
        val removed = buffer.substring(pos - 1, pos)
        buffer.delete(pos - 1, pos)
        undo.record(EditOp.Delete(pos - 1, removed))
        selection = TextRange(pos - 1)
        documentVersion = buffer.documentVersion
    }

    fun deleteForward() {
        if (!selection.collapsed) {
            insert("")
            return
        }
        val pos = selection.start
        if (pos >= buffer.length) return
        val removed = buffer.substring(pos, pos + 1)
        buffer.delete(pos, pos + 1)
        undo.record(EditOp.Delete(pos, removed))
        selection = TextRange(pos)
        documentVersion = buffer.documentVersion
    }

    /**
     * Inserts a newline followed by an indentation derived from the part of
     * the current line that lives **before the caret**. Using only the prefix
     * (not the full line) avoids double-indenting when the caret sits before
     * or inside the existing leading whitespace — the existing content already
     * carries that whitespace, and we'd otherwise prepend a second copy.
     *
     * Smart-indent rules:
     * - If the last non-whitespace character before the caret is `{` or `[`,
     *   an extra [indentUnit] is added so the new line lands one level deeper.
     * - If that opener is **immediately matched** by a closer (`}` or `]`)
     *   right after the caret — i.e. the caret sits between `{|}` or `[|]` —
     *   a *second* newline is inserted with the closing brace pushed onto
     *   its own line at the original indent, and the caret lands in the
     *   middle (indented) line. This is the VS Code / IntelliJ behavior
     *   for "Enter inside an empty block".
     */
    fun insertNewlineWithIndent() {
        val pos = (if (selection.collapsed) selection.end else minOf(selection.start, selection.end))
            .coerceIn(0, buffer.length)
        val lineCol = buffer.offsetToLineCol(pos)
        val current = buffer.contentAt(lineCol.line)
        val before = current.substring(0, lineCol.col.coerceAtMost(current.length))
        val after = current.substring(lineCol.col.coerceAtMost(current.length))

        val baseIndent = buildString {
            for (c in before) {
                if (c == ' ' || c == '\t') append(c) else break
            }
        }

        val openerBefore = (before.length - 1 downTo 0)
            .firstOrNull { before[it] != ' ' && before[it] != '\t' }
            ?.let { before[it] }
        val firstAfter = after.firstOrNull { !it.isWhitespace() }

        val isMatchedPair = (openerBefore == '{' && firstAfter == '}') ||
            (openerBefore == '[' && firstAfter == ']')

        if (isMatchedPair) {
            val firstHalf = "\n" + baseIndent + indentUnit
            val secondHalf = "\n" + baseIndent
            insert(firstHalf + secondHalf)
            selection = TextRange(selection.start - secondHalf.length)
            return
        }

        val extra = if (openerBefore == '{' || openerBefore == '[') indentUnit else ""
        insert("\n" + baseIndent + extra)
    }

    /** Inserts one [indentUnit] at the caret (or replaces the current selection with it). */
    fun insertTab() {
        insert(indentUnit)
    }

    /** Global line index of the caret, used by the renderer for auto-scroll. */
    fun caretLine(): Int = buffer.offsetToLineCol(selection.end).line

    /**
     * Pre-tokenizes the first [maxLines] lines so the cache is warm by the
     * time [CodeEditor] composes the initial visible chunks. Intended to be
     * called from a background coroutine right after constructing the state
     * for a large document — keeps the first frame off the slow path.
     */
    fun prewarmCache(maxLines: Int = 256) {
        val n = minOf(buffer.lineCount, maxLines)
        for (i in 0 until n) {
            cache.get(buffer.lineAt(i))
        }
    }

    fun moveCaret(offset: Int, extend: Boolean) {
        val target = offset.coerceIn(0, buffer.length)
        selection = if (extend) TextRange(selection.start, target) else TextRange(target)
        undo.breakGrouping()
    }

    fun moveCaretBy(delta: Int, extend: Boolean) {
        moveCaret(selection.end + delta, extend)
    }

    fun moveCaretToLineStart(extend: Boolean) {
        val pos = buffer.offsetToLineCol(selection.end)
        moveCaret(buffer.lineStartOffset(pos.line), extend)
    }

    fun moveCaretToLineEnd(extend: Boolean) {
        val pos = buffer.offsetToLineCol(selection.end)
        moveCaret(buffer.lineEndOffset(pos.line), extend)
    }

    fun moveCaretToDocStart(extend: Boolean) {
        moveCaret(0, extend)
    }

    fun moveCaretToDocEnd(extend: Boolean) {
        moveCaret(buffer.length, extend)
    }

    fun moveCaretByLine(deltaLines: Int, extend: Boolean) {
        val pos = buffer.offsetToLineCol(selection.end)
        val targetLine = (pos.line + deltaLines).coerceIn(0, buffer.lineCount - 1)
        val targetCol = pos.col.coerceAtMost(buffer.contentAt(targetLine).length)
        moveCaret(buffer.lineColToOffset(targetLine, targetCol), extend)
    }

    /** Snaps the caret to the next/previous word boundary (Ctrl+Left / Ctrl+Right). */
    fun moveCaretByWord(forward: Boolean, extend: Boolean) {
        val from = selection.end.coerceIn(0, buffer.length)
        val target = if (forward) buffer.nextWordBoundary(from) else buffer.prevWordBoundary(from)
        moveCaret(target, extend)
    }

    /** Ctrl+Delete: removes from the caret up to (but excluding) the next word boundary. */
    fun deleteWordForward() {
        if (!selection.collapsed) { insert(""); return }
        val start = selection.end
        val end = buffer.nextWordBoundary(start)
        if (end <= start) return
        val removed = buffer.substring(start, end)
        buffer.delete(start, end)
        undo.record(EditOp.Delete(start, removed))
        documentVersion = buffer.documentVersion
    }

    /** Ctrl+Backspace: removes from the previous word boundary up to the caret. */
    fun deleteWordBackward() {
        if (!selection.collapsed) { insert(""); return }
        val end = selection.end
        val start = buffer.prevWordBoundary(end)
        if (start >= end) return
        val removed = buffer.substring(start, end)
        buffer.delete(start, end)
        undo.record(EditOp.Delete(start, removed))
        selection = TextRange(start)
        documentVersion = buffer.documentVersion
    }

    /**
     * Moves the currently selected block of lines up or down by one line
     * (Alt+Up / Alt+Down). When the selection is collapsed, only the line
     * containing the caret is moved; otherwise every line touched by the
     * selection moves as a single unit.
     *
     * VS Code-style boundary convention: a selection that ends right at
     * column 0 of a line (i.e. `Shift+Down` from the previous line) does not
     * include that trailing line in the block. This keeps the behavior
     * intuitive when the user extended the selection one line too far.
     *
     * The move is recorded as a single [EditOp.Replace] so a single Ctrl+Z
     * reverts it. The selection shifts by the line delta so it keeps
     * covering the moved block visually.
     */
    fun moveLine(direction: MoveDirection) {
        val low = minOf(selection.start, selection.end)
        val high = maxOf(selection.start, selection.end)
        val lowPos = buffer.offsetToLineCol(low)
        val highPos = buffer.offsetToLineCol(high)

        val blockStartLine = lowPos.line
        val blockEndLine = if (highPos.col == 0 && highPos.line > lowPos.line) {
            highPos.line - 1
        } else {
            highPos.line
        }

        val targetLine = if (direction == MoveDirection.UP) blockStartLine - 1 else blockEndLine + 1
        if (targetLine < 0 || targetLine >= buffer.lineCount) return

        val firstLine = minOf(blockStartLine, targetLine)
        val lastLine = maxOf(blockEndLine, targetLine)
        val rangeStart = buffer.lineStartOffset(firstLine)
        val rangeEnd = buffer.lineEndOffset(lastLine)
        val original = buffer.substring(rangeStart, rangeEnd)

        val blockLines = (blockStartLine..blockEndLine).map { buffer.contentAt(it) }
        val neighbourContent = buffer.contentAt(targetLine)

        val swapped = if (direction == MoveDirection.UP) {
            blockLines.joinToString("\n") + "\n" + neighbourContent
        } else {
            neighbourContent + "\n" + blockLines.joinToString("\n")
        }

        buffer.delete(rangeStart, rangeEnd)
        buffer.insert(rangeStart, swapped)
        undo.record(EditOp.Replace(rangeStart, original, swapped))

        val delta = if (direction == MoveDirection.UP) {
            -(neighbourContent.length + 1)
        } else {
            neighbourContent.length + 1
        }
        selection = TextRange(selection.start + delta, selection.end + delta)
        documentVersion = buffer.documentVersion
    }

    fun selectAll() {
        selection = TextRange(0, buffer.length)
        undo.breakGrouping()
    }

    fun selectionText(): String {
        if (selection.collapsed) return ""
        val s = minOf(selection.start, selection.end)
        val e = maxOf(selection.start, selection.end)
        return buffer.substring(s, e)
    }

    fun undo() {
        val op = undo.popUndo() ?: return
        when (op) {
            is EditOp.Insert -> {
                buffer.delete(op.offset, op.offset + op.text.length)
                selection = TextRange(op.offset)
            }
            is EditOp.Delete -> {
                buffer.insert(op.offset, op.text)
                selection = TextRange(op.offset + op.text.length)
            }
            is EditOp.Replace -> {
                buffer.delete(op.offset, op.offset + op.inserted.length)
                buffer.insert(op.offset, op.removed)
                selection = TextRange(op.offset)
            }
        }
        documentVersion = buffer.documentVersion
    }

    fun redo() {
        val op = undo.popRedo() ?: return
        when (op) {
            is EditOp.Insert -> {
                buffer.insert(op.offset, op.text)
                selection = TextRange(op.offset + op.text.length)
            }
            is EditOp.Delete -> {
                buffer.delete(op.offset, op.offset + op.text.length)
                selection = TextRange(op.offset)
            }
            is EditOp.Replace -> {
                buffer.delete(op.offset, op.offset + op.removed.length)
                buffer.insert(op.offset, op.inserted)
                selection = TextRange(op.offset)
            }
        }
        documentVersion = buffer.documentVersion
    }
}

enum class MoveDirection { UP, DOWN }
