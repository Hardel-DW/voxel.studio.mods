package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry

@Stable
class CodeBlockState {
    val highlights = HighlightRegistry()
    val palette = HighlightPalette()

    private var textState by mutableStateOf("")
    var text: String
        get() = textState
        set(value) {
            textState = value
            refreshHighlights()
        }

    private var highlighterState by mutableStateOf<CodeBlockHighlighter?>(null)
    var highlighter: CodeBlockHighlighter?
        get() = highlighterState
        set(value) {
            highlighterState = value
            if (value == null) highlights.clear() else refreshHighlights()
        }

    var textStyle by mutableStateOf(CODE_TEXT_STYLE)
    var textFill by mutableStateOf(StudioColors.Zinc300)
    var backgroundFill by mutableStateOf(StudioColors.Zinc950)
    var borderFill by mutableStateOf(StudioColors.Zinc800)
    var lineSpacing by mutableStateOf(4.sp)
    var wrapText by mutableStateOf(false)
    var minHeight by mutableStateOf(0.dp)
    var showLineNumbers by mutableStateOf(true)

    internal var renderVersion by mutableIntStateOf(0)
        private set

    init {
        highlights.addListener { renderVersion++ }
        palette.addListener { renderVersion++ }
    }

    fun refreshHighlights() {
        highlights.clear()
        highlighter?.apply(text, highlights)
        renderVersion++
    }
}
