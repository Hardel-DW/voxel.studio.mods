package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface LayoutSpec {
    data class AutoFit(val minColumnWidth: Dp, val maxColumns: Int = Int.MAX_VALUE) : LayoutSpec
    data class Fixed(val frWeights: FloatArray) : LayoutSpec {
        override fun equals(other: Any?) = other is Fixed && frWeights.contentEquals(other.frWeights)
        override fun hashCode() = frWeights.contentHashCode()
    }
}

data class BreakpointRule(
    val minWidth: Dp? = null,
    val maxWidth: Dp? = null,
    val spec: LayoutSpec
) {
    fun matches(width: Dp) =
        (minWidth == null || width >= minWidth) && (maxWidth == null || width <= maxWidth)
}

@Composable
fun ResponsiveGrid(
    items: List<@Composable () -> Unit>,
    defaultSpec: LayoutSpec,
    modifier: Modifier = Modifier,
    rules: List<BreakpointRule> = emptyList(),
    gap: Dp = 16.dp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val width = maxWidth
        val activeSpec = rules.lastOrNull { it.matches(width) }?.spec ?: defaultSpec

        val columns = when (activeSpec) {
            is LayoutSpec.AutoFit -> {
                val fit = ((width + gap) / (activeSpec.minColumnWidth + gap)).toInt().coerceAtLeast(1)
                minOf(items.size, minOf(activeSpec.maxColumns, fit))
            }
            is LayoutSpec.Fixed -> minOf(items.size, activeSpec.frWeights.size)
        }

        if (columns <= 0 || items.isEmpty()) return@BoxWithConstraints

        val rows = (items.size + columns - 1) / columns
        val gapPx = with(density) { gap.roundToPx() }

        SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
            val gridWidth = constraints.maxWidth
            val columnWidths = resolveColumnWidths(
                totalWidth = gridWidth,
                columns = columns,
                gapPx = gapPx,
                spec = activeSpec
            )

            var layoutHeight = 0
            val rowPlaceables = ArrayList<List<androidx.compose.ui.layout.Placeable>>(rows)

            repeat(rows) { row ->
                val rowStart = row * columns
                val rowEnd = minOf(rowStart + columns, items.size)

                val firstPass = (rowStart until rowEnd).map { index ->
                    val column = index - rowStart
                    val itemWidth = columnWidths[column]
                    subcompose("measure-$index") {
                        items[index]()
                    }.single().measure(
                        Constraints(
                            minWidth = itemWidth,
                            maxWidth = itemWidth,
                            minHeight = 0,
                            maxHeight = Constraints.Infinity
                        )
                    )
                }

                val rowHeight = firstPass.maxOfOrNull { it.height } ?: 0
                val forcedRow = (rowStart until rowEnd).map { index ->
                    val column = index - rowStart
                    val itemWidth = columnWidths[column]
                    subcompose("place-$index") {
                        items[index]()
                    }.single().measure(
                        Constraints(
                            minWidth = itemWidth,
                            maxWidth = itemWidth,
                            minHeight = rowHeight,
                            maxHeight = rowHeight
                        )
                    )
                }

                rowPlaceables += forcedRow
                layoutHeight += rowHeight
                if (row < rows - 1) layoutHeight += gapPx
            }

            layout(width = gridWidth, height = layoutHeight) {
                var y = 0
                rowPlaceables.forEachIndexed { rowIndex, placeables ->
                    var x = 0
                    placeables.forEachIndexed { column, placeable ->
                        placeable.placeRelative(x = x, y = y)
                        x += columnWidths[column] + gapPx
                    }
                    val rowHeight = placeables.maxOfOrNull { it.height } ?: 0
                    y += rowHeight
                    if (rowIndex < rowPlaceables.lastIndex) y += gapPx
                }
            }
        }
    }
}

private fun resolveColumnWidths(
    totalWidth: Int,
    columns: Int,
    gapPx: Int,
    spec: LayoutSpec
): IntArray {
    val availableWidth = (totalWidth - gapPx * (columns - 1)).coerceAtLeast(0)
    if (columns == 1) return intArrayOf(availableWidth)

    return when (spec) {
        is LayoutSpec.AutoFit -> {
            val base = availableWidth / columns
            val remainder = availableWidth % columns
            IntArray(columns) { column -> base + if (column < remainder) 1 else 0 }
        }

        is LayoutSpec.Fixed -> {
            val weights = FloatArray(columns) { column ->
                spec.frWeights[minOf(column, spec.frWeights.size - 1)]
            }
            val totalWeight = weights.sum().takeIf { it > 0f } ?: columns.toFloat()
            val result = IntArray(columns)
            var consumed = 0

            for (column in 0 until columns) {
                val width = if (column == columns - 1) {
                    availableWidth - consumed
                } else {
                    ((availableWidth * weights[column]) / totalWeight).toInt()
                }
                result[column] = width
                consumed += width
            }
            result
        }
    }
}
