package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Read-only code viewer with syntax highlighting and selection.
 *
 * Uses [CodeChunkViewStatic] under the hood: the source is split into
 * fixed-line chunks composed lazily, so layout cost is bounded by the
 * viewport rather than the document size.
 */
@Composable
fun CodeBlock(
    state: CodeBlockState,
    modifier: Modifier = Modifier
) {
    val text = state.text
    val renderVersion = state.renderVersion
    val resolvedTextStyle = state.textStyle.merge(
        TextStyle(
            color = state.textFill,
            lineHeight = resolveLineHeight(state.textStyle, state.lineSpacing)
        )
    )

    val markers = remember(text, state.showLineNumbers) {
        buildLineNumberMarkers(text, state.showLineNumbers)
    }
    val source = remember(text, markers) { CodeSource(text, markers) }
    val foregroundRanges = remember(text, renderVersion) {
        buildForegroundHighlightRanges(text, state.highlights, state.palette)
    }
    val paintEntries = remember(renderVersion) {
        buildPaintEntries(state.highlights, state.palette)
    }
    val annotated = remember(text, foregroundRanges, state.textFill) {
        state.precomputedAnnotated ?: buildHighlightedText(text, foregroundRanges, state.textFill)
    }

    CodeChunkViewStatic(
        source = source,
        annotated = annotated,
        paintEntries = paintEntries,
        textStyle = resolvedTextStyle,
        showGutter = state.showLineNumbers,
        backgroundFill = state.backgroundFill,
        borderFill = state.borderFill,
        minHeight = state.minHeight,
        modifier = modifier
    )
}

private fun buildLineNumberMarkers(text: String, showLineNumbers: Boolean): List<CodeLineMarker> {
    val lineCount = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    if (!showLineNumbers) {
        return List(lineCount) { CodeLineMarker(gutterLabel = "", gutterColor = CODE_LINE_NUMBER_COLOR) }
    }
    val maxDigits = lineCount.toString().length
    return List(lineCount) { i ->
        CodeLineMarker(
            gutterLabel = (i + 1).toString().padStart(maxDigits),
            gutterColor = CODE_LINE_NUMBER_COLOR
        )
    }
}
