package fr.hardel.asset_editor.client.compose.components.ui.editor

/**
 * Mutable, line-oriented text buffer that powers [CodeEditor].
 *
 * Storage is an `ArrayList<Line>` where each [Line] carries a stable [Line.id]:
 * editing a line allocates a fresh id for that line, but inserting or removing
 * other lines does **not** disturb the ids of unmodified neighbours. The UI
 * layer can therefore cache derived state (highlighted `AnnotatedString`,
 * tokenization results, …) keyed by [Line.id] and trust the cache to survive
 * insertions, deletions and reorderings elsewhere in the document.
 *
 * Every successful edit bumps [documentVersion]. Use it as a Compose `remember`
 * key when you need to react to *any* change to the buffer.
 *
 * Performance characteristics for the typical Voxel JSON datapack:
 * - Inserting/removing a line is `O(N)` shift in the `ArrayList`; negligible up
 *   to several tens of thousands of lines.
 * - Inserting/deleting characters within a line copies that line's `String`,
 *   `O(L)` where `L` is the line length.
 * - [offsetToLineCol] / [lineColToOffset] are `O(N)`. Caret motion calls them
 *   a handful of times per frame, which is comfortably below any noticeable
 *   threshold up to ~50K lines. A precomputed prefix-sum could be added later
 *   if needed.
 *
 * Thread-safety: not safe. The buffer is owned by a single Compose state.
 */
class EditableLineBuffer(initialText: String) {

    /** A line of text together with a stable identity. */
    class Line internal constructor(
        val id: Long,
        val content: String
    )

    private val _lines = ArrayList<Line>()
    private var nextId: Long = 0L

    /** Increments on every successful edit. Use as a Compose remember key. */
    var documentVersion: Long = 0L
        private set

    val lineCount: Int get() = _lines.size

    /** Total number of characters including the `\n` separators between lines. */
    var length: Int = 0
        private set

    init {
        if (initialText.isEmpty()) {
            _lines += Line(nextId++, "")
        } else {
            for (lineText in initialText.split('\n')) {
                _lines += Line(nextId++, lineText)
                length += lineText.length
            }
            length += _lines.size - 1
        }
    }

    fun lineAt(index: Int): Line = _lines[index]
    fun contentAt(index: Int): String = _lines[index].content
    fun idAt(index: Int): Long = _lines[index].id

    /** Returns a defensive snapshot copy of the lines (debug / tests). */
    fun lineSnapshot(): List<Line> = _lines.toList()

    /** Concatenates every line with `\n` separators. Allocates a single [String]. */
    fun toText(): String {
        val sb = StringBuilder(length)
        for ((index, line) in _lines.withIndex()) {
            if (index > 0) sb.append('\n')
            sb.append(line.content)
        }
        return sb.toString()
    }

    /**
     * Inserts [text] at [offset] (clamped to `[0, length]`). Multi-line input
     * is supported and produces fresh line ids for every line that ends up
     * different from before. Returns the caret offset that should follow the
     * inserted text (`offset + text.length`).
     */
    fun insert(offset: Int, text: String): Int {
        val safeOffset = offset.coerceIn(0, length)
        if (text.isEmpty()) return safeOffset

        val pos = offsetToLineCol(safeOffset)
        val target = _lines[pos.line]
        val before = target.content.substring(0, pos.col)
        val after = target.content.substring(pos.col)
        val combined = before + text + after
        val parts = combined.split('\n')

        if (parts.size == 1) {
            _lines[pos.line] = Line(nextId++, parts[0])
        } else {
            _lines[pos.line] = Line(nextId++, parts[0])
            for (i in 1 until parts.size) {
                _lines.add(pos.line + i, Line(nextId++, parts[i]))
            }
        }

        length += text.length
        documentVersion++
        return safeOffset + text.length
    }

    /**
     * Deletes the characters in `[start, end)` (both clamped to `[0, length]`).
     * When the range spans multiple lines, the surviving fragments of the first
     * and last lines are merged into a single new line. Returns the new caret
     * offset (= the clamped [start]).
     */
    fun delete(start: Int, end: Int): Int {
        val s = start.coerceIn(0, length)
        val e = end.coerceIn(0, length)
        if (e <= s) return s

        val sPos = offsetToLineCol(s)
        val ePos = offsetToLineCol(e)

        if (sPos.line == ePos.line) {
            val line = _lines[sPos.line]
            val newContent = line.content.substring(0, sPos.col) + line.content.substring(ePos.col)
            _lines[sPos.line] = Line(nextId++, newContent)
        } else {
            val firstLine = _lines[sPos.line]
            val lastLine = _lines[ePos.line]
            val merged = firstLine.content.substring(0, sPos.col) + lastLine.content.substring(ePos.col)
            _lines[sPos.line] = Line(nextId++, merged)
            repeat(ePos.line - sPos.line) {
                _lines.removeAt(sPos.line + 1)
            }
        }

        length -= (e - s)
        documentVersion++
        return s
    }

    /**
     * Convenience: deletes `[start, end)` then inserts [text] at the cursor
     * position. Returns the offset just after the inserted text.
     */
    fun replace(start: Int, end: Int, text: String): Int {
        val afterDelete = delete(start, end)
        return insert(afterDelete, text)
    }

    /**
     * Returns the line/column for a global character offset, clamped to a
     * valid position. The result's `col` is always `≤ contentAt(line).length`.
     */
    fun offsetToLineCol(offset: Int): LineCol {
        val target = offset.coerceIn(0, length)
        var remaining = target
        for ((index, line) in _lines.withIndex()) {
            if (remaining <= line.content.length) {
                return LineCol(index, remaining)
            }
            remaining -= line.content.length + 1
        }
        return LineCol(_lines.lastIndex, _lines.last().content.length)
    }

    /** Inverse of [offsetToLineCol]; clamps both [line] and [col] to valid ranges. */
    fun lineColToOffset(line: Int, col: Int): Int {
        val li = line.coerceIn(0, _lines.lastIndex)
        var offset = 0
        for (i in 0 until li) {
            offset += _lines[i].content.length + 1
        }
        val ci = col.coerceIn(0, _lines[li].content.length)
        return offset + ci
    }

    /**
     * Returns the slice `[start, end)` as a fresh [String] without rebuilding
     * the entire document text. Used by edit operations that need to capture
     * what they removed (for the undo stack).
     */
    fun substring(start: Int, end: Int): String {
        val s = start.coerceIn(0, length)
        val e = end.coerceIn(s, length)
        if (e == s) return ""
        val sPos = offsetToLineCol(s)
        val ePos = offsetToLineCol(e)
        if (sPos.line == ePos.line) {
            return _lines[sPos.line].content.substring(sPos.col, ePos.col)
        }
        val sb = StringBuilder(e - s)
        sb.append(_lines[sPos.line].content.substring(sPos.col))
        for (i in (sPos.line + 1) until ePos.line) {
            sb.append('\n')
            sb.append(_lines[i].content)
        }
        sb.append('\n')
        sb.append(_lines[ePos.line].content.substring(0, ePos.col))
        return sb.toString()
    }

    /** Global char offset of the start of [line]. */
    fun lineStartOffset(line: Int): Int = lineColToOffset(line, 0)

    /** Global char offset of the end of [line] (exclusive of any trailing `\n`). */
    fun lineEndOffset(line: Int): Int {
        val li = line.coerceIn(0, _lines.lastIndex)
        return lineColToOffset(li, _lines[li].content.length)
    }

    /**
     * Returns the next word boundary at or after [offset]. Word characters are
     * letters, digits and `_`; runs of whitespace are skipped, then either a
     * full word or a single non-word character is consumed. Crosses line
     * boundaries when the caret sits at the end of a line.
     */
    fun nextWordBoundary(offset: Int): Int {
        val safe = offset.coerceIn(0, length)
        val pos = offsetToLineCol(safe)
        val line = _lines[pos.line].content
        val nextCol = wordBoundaryWithinLine(line, pos.col, forward = true)
        if (nextCol > pos.col) return safe + (nextCol - pos.col)
        if (pos.line + 1 < lineCount) return lineStartOffset(pos.line + 1)
        return length
    }

    /** Symmetric counterpart to [nextWordBoundary]. */
    fun prevWordBoundary(offset: Int): Int {
        val safe = offset.coerceIn(0, length)
        val pos = offsetToLineCol(safe)
        val line = _lines[pos.line].content
        val prevCol = wordBoundaryWithinLine(line, pos.col, forward = false)
        if (prevCol < pos.col) return safe - (pos.col - prevCol)
        if (pos.line > 0) return lineEndOffset(pos.line - 1)
        return 0
    }

    private fun wordBoundaryWithinLine(line: String, col: Int, forward: Boolean): Int {
        if (forward) {
            var p = col.coerceIn(0, line.length)
            while (p < line.length && line[p].isWhitespace()) p++
            if (p < line.length && isWordChar(line[p])) {
                while (p < line.length && isWordChar(line[p])) p++
            } else if (p < line.length) {
                p++
            }
            return p
        }
        var p = col.coerceIn(0, line.length)
        if (p > 0) p--
        while (p > 0 && line[p].isWhitespace()) p--
        if (p > 0 && isWordChar(line[p])) {
            while (p > 0 && isWordChar(line[p - 1])) p--
        }
        return p
    }

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
}

/** A `(line, column)` position inside an [EditableLineBuffer]. */
data class LineCol(val line: Int, val col: Int)
