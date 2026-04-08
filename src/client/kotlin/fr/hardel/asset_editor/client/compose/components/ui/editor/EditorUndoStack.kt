package fr.hardel.asset_editor.client.compose.components.ui.editor

/** A single forward edit operation, used as the unit of undo/redo. */
internal sealed interface EditOp {
    /** Inserted [text] starting at [offset]. */
    data class Insert(val offset: Int, val text: String) : EditOp

    /** Removed [text] starting at [offset] (so the inverse is `insert(offset, text)`). */
    data class Delete(val offset: Int, val text: String) : EditOp

    /**
     * Replaced [removed] with [inserted] in one atomic step (e.g. line move).
     * The inverse swaps the two strings at the same offset.
     */
    data class Replace(val offset: Int, val removed: String, val inserted: String) : EditOp
}

/**
 * Undo / redo stack with rolling-window grouping.
 *
 * Consecutive single-line inserts that continue at the previous offset are
 * merged into one undo step (so typing "hello" undoes in one shot, not five).
 * Likewise, contiguous backspaces collapse into a single delete entry.
 *
 * Grouping is bounded by [groupingWindowMs]: any pause longer than this opens
 * a new undo step. Pressing arrows, clicking elsewhere or running an undo
 * also breaks the group, which matches what users expect from VS Code / IDEA.
 */
internal class EditorUndoStack(
    private val maxSize: Int = 500,
    private val groupingWindowMs: Long = 500L,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val undoStack = ArrayDeque<EditOp>()
    private val redoStack = ArrayDeque<EditOp>()
    private var lastEditTime = 0L

    fun record(op: EditOp) {
        val time = now()
        val withinWindow = time - lastEditTime <= groupingWindowMs
        if (!withinWindow || !tryMerge(op)) {
            undoStack.addLast(op)
            if (undoStack.size > maxSize) undoStack.removeFirst()
        }
        lastEditTime = time
        redoStack.clear()
    }

    /** Pulls the next op to undo (the caller is responsible for applying its inverse). */
    fun popUndo(): EditOp? {
        val op = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(op)
        breakGrouping()
        return op
    }

    /** Pulls the next op to redo (the caller re-applies the forward op). */
    fun popRedo(): EditOp? {
        val op = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(op)
        breakGrouping()
        return op
    }

    /**
     * Forces the next [record] call to start a new group instead of merging
     * with the previous one. Call this on caret motion / pointer click /
     * selection change so the user gets a sensible undo granularity.
     */
    fun breakGrouping() {
        lastEditTime = 0L
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastEditTime = 0L
    }

    /**
     * Three merge cases supported, all gated on "no newlines" so multi-line
     * pastes/inserts always start a fresh undo step:
     * - Consecutive [EditOp.Insert]s typed at the very next position
     * - Consecutive backspaces that each remove the char *before* the previous
     *   delete, collapsing into one growing prefix
     * - Consecutive forward deletes that all remove the char *at* the same
     *   offset, collapsing into one growing suffix
     */
    private fun tryMerge(op: EditOp): Boolean {
        val last = undoStack.lastOrNull() ?: return false
        return when (last) {
            is EditOp.Insert if op is EditOp.Insert &&
                    op.offset == last.offset + last.text.length &&
                    !op.text.contains('\n') && !last.text.contains('\n') -> {
                undoStack.removeLast()
                undoStack.addLast(EditOp.Insert(last.offset, last.text + op.text))
                true
            }
            is EditOp.Delete if op is EditOp.Delete &&
                    op.offset + op.text.length == last.offset &&
                    !op.text.contains('\n') && !last.text.contains('\n') -> {
                undoStack.removeLast()
                undoStack.addLast(EditOp.Delete(op.offset, op.text + last.text))
                true
            }
            is EditOp.Delete if op is EditOp.Delete &&
                    op.offset == last.offset &&
                    !op.text.contains('\n') && !last.text.contains('\n') -> {
                undoStack.removeLast()
                undoStack.addLast(EditOp.Delete(last.offset, last.text + op.text))
                true
            }

            else -> false
        }
    }
}
