package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography

data class TableColumn<T>(
    val header: String,
    val weight: Float = 1f,
    val fixedWidth: Boolean = false,
    val cell: @Composable RowScope.(T) -> Unit
)

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
    expandContent: (@Composable (T) -> Unit)? = null
) {
    val paginated = pageSize > 0 && items.size > pageSize
    val totalPages = if (paginated) (items.size + pageSize - 1) / pageSize else 1
    val visibleItems = if (paginated) {
        val start = currentPage * pageSize
        items.subList(start, minOf(start + pageSize, items.size))
    } else items

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp)
        ) {
            for (col in columns) {
                Box(modifier = Modifier.weight(col.weight)) {
                    Text(
                        text = col.header.uppercase(),
                        style = VoxelTypography.medium(11),
                        color = VoxelColors.Zinc500
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Text(
                    text = placeholder,
                    style = VoxelTypography.medium(14),
                    color = VoxelColors.Zinc500
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                visibleItems.forEachIndexed { index, item ->
                    val rowId = idExtractor?.invoke(item) ?: index.toLong()
                    val isExpanded = expandedIds.contains(rowId)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp)
                                .then(if (index % 2 == 1) Modifier.background(VoxelColors.Zinc950.copy(alpha = 0.5f)) else Modifier)
                                .padding(horizontal = 16.dp)
                                .then(
                                    if (expandContent != null) Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onToggleExpand?.invoke(rowId) }
                                    else Modifier
                                )
                        ) {
                            for (col in columns) {
                                Box(modifier = Modifier.weight(col.weight)) {
                                    col.cell(this@Row, item)
                                }
                            }
                        }

                        if (isExpanded && expandContent != null) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                expandContent(item)
                            }
                        }
                    }
                }
            }
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
