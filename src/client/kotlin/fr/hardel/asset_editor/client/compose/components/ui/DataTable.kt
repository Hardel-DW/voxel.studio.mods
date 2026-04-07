package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

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

    val tableShape = RoundedCornerShape(8.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(StudioColors.Zinc900.copy(alpha = 0.4f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 16.dp)
        ) {
            for (col in columns) {
                Box(modifier = Modifier.weight(col.weight)) {
                    Text(
                        text = col.header.uppercase(),
                        style = StudioTypography.medium(11),
                        color = StudioColors.Zinc500
                    )
                }
            }
        }

        if (items.isEmpty()) {
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                visibleItems.forEachIndexed { index, item ->
                    val rowId = idExtractor?.invoke(item) ?: index.toLong()
                    val isExpanded = expandedIds.contains(rowId)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val isEven = index % 2 == 0

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
                                .heightIn(min = 36.dp)
                                .background(
                                    when {
                                        isHovered -> StudioColors.Zinc800.copy(alpha = 0.4f)
                                        isEven -> StudioColors.Zinc900.copy(alpha = 0.15f)
                                        else -> StudioColors.Zinc950.copy(alpha = 0.3f)
                                    }
                                )
                                .hoverable(interactionSource)
                                .padding(horizontal = 16.dp)
                                .then(
                                    if (expandContent != null) Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(
                                            interactionSource = interactionSource,
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
