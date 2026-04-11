package fr.hardel.asset_editor.client.compose.components.ui.datatable

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput

internal data class DataTableRowBounds(
    val topInRoot: Float,
    val bottomInRoot: Float
)

/** Short click, long press (>=400ms), or drag across >=2 rows with live feedback. */
internal fun Modifier.selectionGesture(
    rowId: Long,
    allRowIds: List<Long>,
    rowBoundsById: Map<Long, DataTableRowBounds>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onToggleExpand: (() -> Unit)?,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onSelectionUpdate: (Set<Long>) -> Unit
): Modifier = composed {
    val currentRowBoundsById = rememberUpdatedState(rowBoundsById)
    val currentSelectedIds = rememberUpdatedState(selectedIds)
    val currentSelectionMode = rememberUpdatedState(selectionMode)
    val currentToggleExpand = rememberUpdatedState(onToggleExpand)
    val currentToggleSelect = rememberUpdatedState(onToggleSelect)
    val currentLongPress = rememberUpdatedState(onLongPress)
    val currentSelectionUpdate = rememberUpdatedState(onSelectionUpdate)

    pointerInput(rowId, allRowIds) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val baseSelection = currentSelectedIds.value
            var dragStarted = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                val targetRowId = findTargetRowId(
                    startRowId = rowId,
                    pointerYInRoot = pointer.position.y,
                    allRowIds = allRowIds,
                    rowBoundsById = currentRowBoundsById.value
                )

                if (targetRowId != null && targetRowId != rowId) {
                    dragStarted = true
                    currentSelectionUpdate.value(baseSelection.toggle(selectionRange(rowId, targetRowId, allRowIds)))
                    pointer.consume()
                }

                if (pointer.pressed && !pointer.changedToUp()) continue

                pointer.consume()
                if (!dragStarted) {
                    val heldLongEnough = pointer.uptimeMillis - down.uptimeMillis >= 400
                    when {
                        heldLongEnough && !currentSelectionMode.value && currentToggleExpand.value != null -> currentLongPress.value()
                        currentSelectionMode.value || currentToggleExpand.value == null -> currentToggleSelect.value()
                        else -> currentToggleExpand.value?.invoke()
                    }
                }
                break
            }
        }
    }
}

private fun findTargetRowId(
    startRowId: Long,
    pointerYInRoot: Float,
    allRowIds: List<Long>,
    rowBoundsById: Map<Long, DataTableRowBounds>
): Long? {
    val startBounds = rowBoundsById[startRowId] ?: return null
    val absolutePointerY = startBounds.topInRoot + pointerYInRoot
    val measuredRows = allRowIds.mapNotNull { id -> rowBoundsById[id]?.let { id to it } }
    if (measuredRows.isEmpty()) return null

    measuredRows.firstOrNull { (_, bounds) ->
        absolutePointerY >= bounds.topInRoot && absolutePointerY < bounds.bottomInRoot
    }?.let { return it.first }

    if (absolutePointerY < measuredRows.first().second.topInRoot) return measuredRows.first().first
    if (absolutePointerY >= measuredRows.last().second.bottomInRoot) return measuredRows.last().first
    return null
}

private fun selectionRange(
    startRowId: Long,
    targetRowId: Long,
    allRowIds: List<Long>
): Set<Long> {
    val startIndex = allRowIds.indexOf(startRowId)
    val endIndex = allRowIds.indexOf(targetRowId)
    if (startIndex == -1 || endIndex == -1) return emptySet()

    return buildSet {
        for (index in minOf(startIndex, endIndex)..maxOf(startIndex, endIndex)) {
            add(allRowIds[index])
        }
    }
}

private fun Set<Long>.toggle(ids: Set<Long>): Set<Long> {
    if (ids.isEmpty()) return this

    val toggled = toMutableSet()
    ids.forEach { id ->
        if (!toggled.add(id)) {
            toggled.remove(id)
        }
    }
    return toggled
}
