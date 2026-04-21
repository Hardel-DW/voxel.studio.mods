package fr.hardel.asset_editor.client.compose.components.ui.datatable

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Pagination

@Composable
fun <T> DataTable(
    items: List<T>,
    columns: List<TableColumn<T>>,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    pageSize: Int = -1,
    currentPage: Int = 0,
    onPageChange: ((Int) -> Unit)? = null,
    idExtractor: ((T) -> Long)? = null,
    expandedIds: Set<Long> = emptySet(),
    onToggleExpand: ((Long) -> Unit)? = null,
    expandContent: (@Composable (T) -> Unit)? = null,
    selectedIds: Set<Long> = emptySet(),
    onSelectionChange: ((Set<Long>) -> Unit)? = null,
    lazy: Boolean = false
) {
    val rowBoundsById = remember { mutableStateMapOf<Long, DataTableRowBounds>() }
    val selectionMode = selectedIds.isNotEmpty()
    val paginated = pageSize > 0 && items.size > pageSize
    val totalPages = if (paginated) (items.size + pageSize - 1) / pageSize else 1
    val pageOffset = if (paginated) currentPage * pageSize else 0
    val visibleItems = if (paginated) {
        val start = currentPage * pageSize
        items.subList(start, minOf(start + pageSize, items.size))
    } else items

    val rowIds = remember(visibleItems, idExtractor, pageOffset) {
        visibleItems.mapIndexed { index, item -> idExtractor?.invoke(item) ?: (pageOffset + index).toLong() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        DataTableHeader(columns, showCheckboxColumn = selectionMode)

        when {
            items.isEmpty() -> EmptyPlaceholder(placeholder)
            lazy -> LazyDataTableBody(
                modifier = Modifier.weight(1f),
                visibleItems = visibleItems,
                columns = columns,
                rowIds = rowIds,
                rowBoundsById = rowBoundsById,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onSelectionChange = onSelectionChange,
                expandedIds = expandedIds,
                onToggleExpand = onToggleExpand,
                expandContent = expandContent
            )
            else -> ScrollableDataTableBody(
                visibleItems = visibleItems,
                columns = columns,
                rowIds = rowIds,
                rowBoundsById = rowBoundsById,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onSelectionChange = onSelectionChange,
                expandedIds = expandedIds,
                onToggleExpand = onToggleExpand,
                expandContent = expandContent
            )
        }

        if (paginated && onPageChange != null) {
            Pagination(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = onPageChange,
                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally)
            )
        }
    }
}

private val BODY_SHAPE = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)

@Composable
private fun <T> LazyDataTableBody(
    modifier: Modifier,
    visibleItems: List<T>,
    columns: List<TableColumn<T>>,
    rowIds: List<Long>,
    rowBoundsById: MutableMap<Long, DataTableRowBounds>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onSelectionChange: ((Set<Long>) -> Unit)?,
    expandedIds: Set<Long>,
    onToggleExpand: ((Long) -> Unit)?,
    expandContent: (@Composable (T) -> Unit)?
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(BODY_SHAPE)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), BODY_SHAPE)
    ) {
        itemsIndexed(visibleItems, key = { index, _ -> rowIds[index] }) { index, item ->
            val rowId = rowIds[index]
            DataTableRow(
                item = item,
                index = index,
                columns = columns,
                rowId = rowId,
                allRowIds = rowIds,
                rowBoundsById = rowBoundsById,
                isEven = index % 2 == 0,
                isSelected = selectedIds.contains(rowId),
                isExpanded = expandedIds.contains(rowId),
                selectionMode = selectionMode,
                selectable = onSelectionChange != null,
                selectedIds = selectedIds,
                onSelectionChange = onSelectionChange,
                onToggleExpand = onToggleExpand,
                onRowBoundsChange = { bounds -> rowBoundsById[rowId] = bounds },
                expandContent = expandContent
            )
        }
    }
}

@Composable
private fun <T> ScrollableDataTableBody(
    visibleItems: List<T>,
    columns: List<TableColumn<T>>,
    rowIds: List<Long>,
    rowBoundsById: MutableMap<Long, DataTableRowBounds>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onSelectionChange: ((Set<Long>) -> Unit)?,
    expandedIds: Set<Long>,
    onToggleExpand: ((Long) -> Unit)?,
    expandContent: (@Composable (T) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BODY_SHAPE)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), BODY_SHAPE)
            .verticalScroll(rememberScrollState())
    ) {
        visibleItems.forEachIndexed { index, item ->
            val rowId = rowIds[index]
            DataTableRow(
                item = item,
                index = index,
                columns = columns,
                rowId = rowId,
                allRowIds = rowIds,
                rowBoundsById = rowBoundsById,
                isEven = index % 2 == 0,
                isSelected = selectedIds.contains(rowId),
                isExpanded = expandedIds.contains(rowId),
                selectionMode = selectionMode,
                selectable = onSelectionChange != null,
                selectedIds = selectedIds,
                onSelectionChange = onSelectionChange,
                onToggleExpand = onToggleExpand,
                onRowBoundsChange = { bounds -> rowBoundsById[rowId] = bounds },
                expandContent = expandContent
            )
        }
    }
}

@Composable
private fun EmptyPlaceholder(placeholder: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .padding(32.dp)
    ) {
        Text(
            text = placeholder,
            style = StudioTypography.medium(14),
            color = StudioColors.Zinc500
        )
    }
}
