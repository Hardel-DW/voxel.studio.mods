package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonElement

enum class HistorySource { Ui, Code }

/**
 * Linear undo/redo history over a single JsonElement state. Consecutive
 * replacements within [groupingWindowMs] collapse into one undo step so
 * keystroke-level edits don't burn through the stack — matches the rolling
 * window of [fr.hardel.asset_editor.client.compose.components.ui.editor.EditorUndoStack].
 *
 * [lastSource] tracks the origin of the latest change. Sync effects can read
 * it to decide whether to push the new value back to the side that produced
 * it (which would clobber the user's caret). Undo/redo always report [HistorySource.Ui]
 * because they are global keybinding actions, not typed input in any panel.
 */
@Stable
class McdocHistory(
    initial: JsonElement,
    private val maxSize: Int = 200,
    private val groupingWindowMs: Long = 500L,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val past = ArrayDeque<JsonElement>()
    private val future = ArrayDeque<JsonElement>()
    private var lastEditTime = 0L

    var current by mutableStateOf(initial)
        private set

    var lastSource by mutableStateOf(HistorySource.Ui)
        private set

    fun replace(next: JsonElement, source: HistorySource = HistorySource.Ui) {
        if (next == current) return
        val time = now()
        val grouping = time - lastEditTime <= groupingWindowMs && past.isNotEmpty()
        if (!grouping) {
            past.addLast(current)
            if (past.size > maxSize) past.removeFirst()
        }
        future.clear()
        current = next
        lastSource = source
        lastEditTime = time
    }

    fun reset(value: JsonElement) {
        past.clear()
        future.clear()
        lastEditTime = 0L
        current = value
        lastSource = HistorySource.Ui
    }

    fun undo(): Boolean {
        val previous = past.removeLastOrNull() ?: return false
        future.addLast(current)
        lastEditTime = 0L
        current = previous
        lastSource = HistorySource.Ui
        return true
    }

    fun redo(): Boolean {
        val next = future.removeLastOrNull() ?: return false
        past.addLast(current)
        lastEditTime = 0L
        current = next
        lastSource = HistorySource.Ui
        return true
    }
}
