package fr.hardel.asset_editor.client.compose.components.ui.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.Checkbox

private val ROW_HEIGHT = 40.dp

@Composable
internal fun <T> DataTableRow(
    item: T,
    index: Int,
    columns: List<TableColumn<T>>,
    rowId: Long,
    allRowIds: List<Long>,
    isEven: Boolean,
    isSelected: Boolean,
    isExpanded: Boolean,
    selectionMode: Boolean,
    selectable: Boolean,
    selectedIds: Set<Long>,
    onSelectionChange: ((Set<Long>) -> Unit)?,
    onToggleExpand: ((Long) -> Unit)?,
    expandContent: (@Composable (T) -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val density = LocalDensity.current
    val rowHeightPx = with(density) { (ROW_HEIGHT + 1.dp).toPx() }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (index > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StudioColors.Zinc800.copy(alpha = 0.3f))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_HEIGHT)
                .background(rowBackground(isSelected, isHovered, isEven))
                .hoverable(interactionSource)
                .padding(horizontal = 16.dp)
                .then(rowInteraction(selectionMode, selectable, rowId, index, allRowIds, rowHeightPx, selectedIds, onSelectionChange, onToggleExpand))
        ) {
            if (selectionMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(32.dp)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            onSelectionChange?.invoke(
                                if (checked) selectedIds + rowId else selectedIds - rowId
                            )
                        }
                    )
                }
            }
            for (col in columns) {
                Box(modifier = Modifier.weight(col.weight)) {
                    col.cell(this@Row, item)
                }
            }
        }

        if (isExpanded && expandContent != null && !selectionMode) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                expandContent(item)
            }
        }
    }
}

private fun rowBackground(isSelected: Boolean, isHovered: Boolean, isEven: Boolean) = when {
    isSelected -> StudioColors.Zinc700.copy(alpha = 0.3f)
    isHovered -> StudioColors.Zinc800.copy(alpha = 0.4f)
    isEven -> StudioColors.Zinc900.copy(alpha = 0.15f)
    else -> StudioColors.Zinc950.copy(alpha = 0.3f)
}

private fun rowInteraction(
    selectionMode: Boolean,
    selectable: Boolean,
    rowId: Long,
    index: Int,
    allRowIds: List<Long>,
    rowHeightPx: Float,
    selectedIds: Set<Long>,
    onSelectionChange: ((Set<Long>) -> Unit)?,
    onToggleExpand: ((Long) -> Unit)?
): Modifier {
    if (onSelectionChange == null) {
        if (onToggleExpand == null) return Modifier
        return Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onToggleExpand(rowId) }
    }

    if (!selectable) return Modifier

    return Modifier
        .pointerHoverIcon(PointerIcon.Hand)
        .selectionGesture(
            rowId = rowId,
            rowIndex = index,
            allRowIds = allRowIds,
            rowHeightPx = rowHeightPx,
            selectedIds = selectedIds,
            selectionMode = selectionMode,
            onToggleExpand = { onToggleExpand?.invoke(rowId) },
            onToggleSelect = {
                val isSelected = selectedIds.contains(rowId)
                onSelectionChange(if (isSelected) selectedIds - rowId else selectedIds + rowId)
            },
            onLongPress = { onSelectionChange(selectedIds + rowId) },
            onSelectionUpdate = onSelectionChange
        )
}
