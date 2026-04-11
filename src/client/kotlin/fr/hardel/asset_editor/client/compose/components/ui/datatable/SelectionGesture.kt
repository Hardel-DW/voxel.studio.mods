package fr.hardel.asset_editor.client.compose.components.ui.datatable

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput

/** Short click, long press (≥400ms), or drag across ≥2 rows with live feedback. */
internal fun Modifier.selectionGesture(
    rowId: Long,
    rowIndex: Int,
    allRowIds: List<Long>,
    rowHeightPx: Float,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onSelectionUpdate: (Set<Long>) -> Unit
): Modifier = composed {
    val currentSelectedIds = rememberUpdatedState(selectedIds)
    val currentSelectionMode = rememberUpdatedState(selectionMode)
    val currentToggleExpand = rememberUpdatedState(onToggleExpand)
    val currentToggleSelect = rememberUpdatedState(onToggleSelect)
    val currentLongPress = rememberUpdatedState(onLongPress)
    val currentSelectionUpdate = rememberUpdatedState(onSelectionUpdate)

    pointerInput(rowId, allRowIds) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val startY = down.position.y
            val startTime = System.currentTimeMillis()
            val baseSelection = currentSelectedIds.value
            val startRowSelected = baseSelection.contains(rowId)
            var crossedOtherRow = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val pointer = event.changes.firstOrNull() ?: break

                val deltaY = pointer.position.y - startY
                val deltaRows = if (deltaY >= 0) (deltaY / rowHeightPx).toInt() else -(-deltaY / rowHeightPx).toInt()
                val targetIndex = (rowIndex + deltaRows).coerceIn(0, allRowIds.size - 1)

                if (targetIndex != rowIndex && !crossedOtherRow) crossedOtherRow = true

                if (crossedOtherRow) {
                    val range = minOf(rowIndex, targetIndex)..maxOf(rowIndex, targetIndex)
                    val dragIds = range.map { allRowIds[it] }.toSet()
                    val newSelection = if (startRowSelected) baseSelection - dragIds else baseSelection + dragIds
                    currentSelectionUpdate.value(newSelection)
                    if (pointer.changedToUp()) {
                        pointer.consume()
                        break
                    }
                } else if (pointer.changedToUp()) {
                    pointer.consume()
                    if (System.currentTimeMillis() - startTime >= 400) {
                        currentLongPress.value()
                    } else if (currentSelectionMode.value) {
                        currentToggleSelect.value()
                    } else {
                        currentToggleExpand.value()
                    }
                    break
                }
            }
        }
    }
}
