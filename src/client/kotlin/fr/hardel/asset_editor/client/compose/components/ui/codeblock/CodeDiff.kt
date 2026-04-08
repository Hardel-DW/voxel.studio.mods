package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry
import fr.hardel.asset_editor.client.compose.lib.utils.DiffComputer
import fr.hardel.asset_editor.client.compose.lib.utils.DiffLine
import fr.hardel.asset_editor.client.compose.lib.utils.DiffLineType

enum class DiffStatus { ADDED, DELETED, UPDATED }

private val ADDED_BG = Color(0xFF22C55E).copy(alpha = 0.10f)
private val ADDED_GUTTER_BG = Color(0xFF22C55E).copy(alpha = 0.05f)
private val ADDED_GUTTER_TEXT = Color(0xFF22C55E)
private val REMOVED_BG = Color(0xFFEF4444).copy(alpha = 0.10f)
private val REMOVED_GUTTER_BG = Color(0xFFEF4444).copy(alpha = 0.05f)
private val REMOVED_GUTTER_TEXT = Color(0xFFEF4444)

@Composable
fun CodeDiff(
    original: String,
    compiled: String,
    status: DiffStatus,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = CODE_TEXT_STYLE,
    lineSpacing: Float = 4f
) {
    val diffLines = remember(original, compiled, status) {
        when (status) {
            DiffStatus.ADDED -> DiffComputer.computeFullDiff(compiled, DiffLineType.ADDED)
            DiffStatus.DELETED -> DiffComputer.computeFullDiff(original, DiffLineType.REMOVED)
            DiffStatus.UPDATED -> DiffComputer.computeUnifiedDiff(original, compiled)
        }
    }
    val fullText = remember(diffLines) {
        diffLines.joinToString("\n") { it.content.ifEmpty { "\u00A0" } }
    }
    val markers = remember(diffLines) { buildDiffMarkers(diffLines) }
    val source = remember(fullText, markers) { CodeSource(fullText, markers) }

    val palette = remember { HighlightPalette().also { JsonCodeBlockHighlighter.installDefaultPalette(it) } }
    val highlightRegistry = remember(fullText) {
        HighlightRegistry().also { JsonCodeBlockHighlighter().apply(fullText, it) }
    }
    val resolvedStyle = textStyle.merge(
        TextStyle(
            color = StudioColors.Zinc300,
            lineHeight = (textStyle.fontSize.value + lineSpacing).sp
        )
    )
    val foregroundRanges = remember(fullText, highlightRegistry, palette) {
        buildForegroundHighlightRanges(fullText, highlightRegistry, palette)
    }
    val paintEntries = remember(highlightRegistry, palette) {
        buildPaintEntries(highlightRegistry, palette)
    }
    val annotated = remember(fullText, foregroundRanges, resolvedStyle.color) {
        buildHighlightedText(fullText, foregroundRanges, resolvedStyle.color)
    }
    val chunkAnnotated = remember(annotated, source) {
        { chunkIndex: Int -> sliceAnnotatedToChunk(annotated, source, chunkIndex) }
    }
    val chunkVersion = remember { { _: Int -> 0L } }

    var selection by remember(fullText) { mutableStateOf(TextRange.Zero) }
    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()
    val focusRequester = remember(fullText) { FocusRequester() }

    CodeChunkView(
        source = source,
        chunkAnnotated = chunkAnnotated,
        chunkVersion = chunkVersion,
        paintEntries = paintEntries,
        textStyle = resolvedStyle,
        showGutter = true,
        backgroundFill = StudioColors.Zinc950,
        borderFill = StudioColors.Zinc800,
        minHeight = 0.dp,
        selection = selection,
        onSelectionChange = { selection = it },
        lazyListState = lazyListState,
        horizontalScrollState = horizontalScroll,
        focusRequester = focusRequester,
        modifier = modifier
    )
}

private fun buildDiffMarkers(lines: List<DiffLine>): List<CodeLineMarker> {
    val maxNumber = lines.mapNotNull { it.lineNumber }.maxOrNull() ?: 1
    val maxDigits = maxNumber.toString().length
    return lines.map { line ->
        when (line.type) {
            DiffLineType.ADDED -> CodeLineMarker(
                gutterLabel = "+".padStart(maxDigits),
                gutterColor = ADDED_GUTTER_TEXT,
                lineBackground = ADDED_BG,
                gutterBackground = ADDED_GUTTER_BG
            )
            DiffLineType.REMOVED -> CodeLineMarker(
                gutterLabel = "-".padStart(maxDigits),
                gutterColor = REMOVED_GUTTER_TEXT,
                lineBackground = REMOVED_BG,
                gutterBackground = REMOVED_GUTTER_BG
            )
            DiffLineType.UNCHANGED -> CodeLineMarker(
                gutterLabel = (line.lineNumber?.toString() ?: "").padStart(maxDigits),
                gutterColor = CODE_LINE_NUMBER_COLOR
            )
        }
    }
}
