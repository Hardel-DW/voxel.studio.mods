package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ToolbarSize(val width: Dp, val height: Dp) {
    FIT(700.dp, 320.dp),
    LARGE(800.dp, 500.dp)
}

sealed class FloatingBarExpansion {
    data object Collapsed : FloatingBarExpansion()
    data class Expanded(val content: @Composable () -> Unit, val size: ToolbarSize) : FloatingBarExpansion()
}

class FloatingBarState {
    var expansion by mutableStateOf<FloatingBarExpansion>(FloatingBarExpansion.Collapsed)
        private set

    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)

    val isExpanded: Boolean get() = expansion is FloatingBarExpansion.Expanded

    fun expand(size: ToolbarSize = ToolbarSize.LARGE, content: @Composable () -> Unit) {
        expansion = FloatingBarExpansion.Expanded(content, size)
    }

    fun collapse() {
        if (expansion is FloatingBarExpansion.Expanded)
            expansion = FloatingBarExpansion.Collapsed
    }

    fun resize(size: ToolbarSize) {
        val current = expansion
        if (current is FloatingBarExpansion.Expanded)
            expansion = current.copy(size = size)
    }

    fun resetPosition() {
        offsetX = 0f
        offsetY = 0f
    }
}

val LocalFloatingBar = compositionLocalOf<FloatingBarState> {
    error("No FloatingBarState provided")
}
