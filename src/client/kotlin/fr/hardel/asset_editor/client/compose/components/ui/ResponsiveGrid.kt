package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        val weight = when (activeSpec) {
                            is LayoutSpec.Fixed -> activeSpec.frWeights[minOf(col, activeSpec.frWeights.size - 1)]
                            is LayoutSpec.AutoFit -> 1f
                        }

                        Box(modifier = Modifier.weight(weight)) {
                            if (index < items.size) {
                                items[index]()
                            }
                        }
                    }
                }
            }
        }
    }
}
