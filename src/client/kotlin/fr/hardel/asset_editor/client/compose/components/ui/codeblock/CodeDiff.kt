package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry
import fr.hardel.asset_editor.client.compose.lib.utils.DiffComputer
import fr.hardel.asset_editor.client.compose.lib.utils.DiffLine
import fr.hardel.asset_editor.client.compose.lib.utils.DiffLineType

enum class DiffStatus { ADDED, DELETED, UPDATED }

private val DIFF_SHAPE = RoundedCornerShape(10.dp)
private val ADDED_BG = Color(0xFF22C55E).copy(alpha = 0.10f)
private val ADDED_GUTTER_BG = Color(0xFF22C55E).copy(alpha = 0.05f)
private val ADDED_GUTTER_TEXT = Color(0xFF22C55E)
private val REMOVED_BG = Color(0xFFEF4444).copy(alpha = 0.10f)
private val REMOVED_GUTTER_BG = Color(0xFFEF4444).copy(alpha = 0.05f)
private val REMOVED_GUTTER_TEXT = Color(0xFFEF4444)
private val GUTTER_BORDER = StudioColors.Zinc800.copy(alpha = 0.5f)
private val LINE_NUMBER_COLOR = StudioColors.Zinc600
private val GUTTER_VERTICAL_PADDING = 16.dp
private val GUTTER_HORIZONTAL_PADDING = 12.dp

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
    val palette = remember { HighlightPalette().also { JsonCodeBlockHighlighter.installDefaultPalette(it) } }
    val highlighter = remember { JsonCodeBlockHighlighter() }
    val highlightRegistry = remember(fullText, highlighter) {
        HighlightRegistry().also { highlighter.apply(fullText, it) }
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
    val highlightedAnnotated = remember(fullText, foregroundRanges, resolvedStyle.color) {
        buildHighlightedText(fullText, foregroundRanges, resolvedStyle.color)
    }
    val transformation = remember(highlightedAnnotated) {
        CachedHighlightTransformation(highlightedAnnotated)
    }
    val gutterText = remember(diffLines) { buildGutterText(diffLines) }

    var textFieldValue by remember(fullText) { mutableStateOf(TextFieldValue(fullText)) }
    LaunchedEffect(fullText) {
        if (textFieldValue.text != fullText) {
            val sel = TextRange(
                textFieldValue.selection.start.coerceIn(0, fullText.length),
                textFieldValue.selection.end.coerceIn(0, fullText.length)
            )
            textFieldValue = textFieldValue.copy(text = fullText, selection = sel)
        }
    }

    var contentLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var gutterLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .clip(DIFF_SHAPE)
            .background(StudioColors.Zinc950)
            .border(1.dp, StudioColors.Zinc800, DIFF_SHAPE)
    ) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DiffGutter(
                    lines = diffLines,
                    gutterText = gutterText,
                    textStyle = resolvedStyle,
                    viewportHeight = viewportHeight,
                    layoutResult = gutterLayoutResult,
                    onLayoutResult = { gutterLayoutResult = it }
                )
                DiffContent(
                    lines = diffLines,
                    fullText = fullText,
                    paintEntries = paintEntries,
                    textStyle = resolvedStyle,
                    transformation = transformation,
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = { updated ->
                        textFieldValue = if (updated.text == fullText) updated else updated.copy(text = fullText)
                    },
                    viewportHeight = viewportHeight,
                    layoutResult = contentLayoutResult,
                    onLayoutResult = { contentLayoutResult = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DiffGutter(
    lines: List<DiffLine>,
    gutterText: AnnotatedString,
    textStyle: TextStyle,
    viewportHeight: Dp,
    layoutResult: TextLayoutResult?,
    onLayoutResult: (TextLayoutResult) -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = viewportHeight)
            .drawBehind {
                drawRect(
                    color = GUTTER_BORDER,
                    topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                    size = Size(1.dp.toPx(), size.height)
                )
                val layout = layoutResult ?: return@drawBehind
                val padTopPx = GUTTER_VERTICAL_PADDING.toPx()
                for (i in lines.indices) {
                    val bg = when (lines[i].type) {
                        DiffLineType.ADDED -> ADDED_GUTTER_BG
                        DiffLineType.REMOVED -> REMOVED_GUTTER_BG
                        DiffLineType.UNCHANGED -> continue
                    }
                    val top = padTopPx + layout.getLineTop(i)
                    val bottom = padTopPx + layout.getLineBottom(i)
                    drawRect(bg, topLeft = Offset(0f, top), size = Size(size.width, bottom - top))
                }
            }
    ) {
        BasicText(
            text = gutterText,
            style = textStyle.copy(color = LINE_NUMBER_COLOR),
            onTextLayout = onLayoutResult,
            modifier = Modifier.padding(
                top = GUTTER_VERTICAL_PADDING,
                bottom = GUTTER_VERTICAL_PADDING,
                start = GUTTER_HORIZONTAL_PADDING,
                end = GUTTER_HORIZONTAL_PADDING
            )
        )
    }
}

@Composable
private fun DiffContent(
    lines: List<DiffLine>,
    fullText: String,
    paintEntries: List<PaintHighlightEntry>,
    textStyle: TextStyle,
    transformation: VisualTransformation,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    viewportHeight: Dp,
    layoutResult: TextLayoutResult?,
    onLayoutResult: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val contentMinWidth: Dp = (maxWidth - CODE_BLOCK_CONTENT_PADDING * 2).coerceAtLeast(0.dp)
        val contentMinHeight: Dp = (viewportHeight - CODE_BLOCK_CONTENT_PADDING * 2).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onTextFieldValueChange,
                readOnly = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.Transparent),
                visualTransformation = transformation,
                onTextLayout = onLayoutResult,
                modifier = Modifier
                    .widthIn(min = contentMinWidth)
                    .heightIn(min = contentMinHeight)
                    .padding(CODE_BLOCK_CONTENT_PADDING)
                    .drawWithContent {
                        val layout = layoutResult ?: run {
                            drawContent()
                            return@drawWithContent
                        }
                        val padPx = CODE_BLOCK_CONTENT_PADDING.toPx()
                        for (i in lines.indices) {
                            val bg = when (lines[i].type) {
                                DiffLineType.ADDED -> ADDED_BG
                                DiffLineType.REMOVED -> REMOVED_BG
                                DiffLineType.UNCHANGED -> continue
                            }
                            val top = padPx + layout.getLineTop(i)
                            val bottom = padPx + layout.getLineBottom(i)
                            drawRect(bg, topLeft = Offset(0f, top), size = Size(size.width, bottom - top))
                        }
                        translate(left = padPx, top = padPx) {
                            drawHighlightBackgrounds(fullText, layout, paintEntries)
                        }
                        drawContent()
                        translate(left = padPx, top = padPx) {
                            drawHighlightUnderlines(fullText, layout, paintEntries)
                        }
                    }
            )
        }
    }
}

private fun buildGutterText(lines: List<DiffLine>): AnnotatedString {
    val maxDigits = lines.mapNotNull { it.lineNumber }.maxOrNull()?.toString()?.length ?: 1

    return buildAnnotatedString {
        for ((index, line) in lines.withIndex()) {
            if (index > 0) append("\n")

            val color = when (line.type) {
                DiffLineType.ADDED -> ADDED_GUTTER_TEXT
                DiffLineType.REMOVED -> REMOVED_GUTTER_TEXT
                DiffLineType.UNCHANGED -> LINE_NUMBER_COLOR
            }
            val label = when (line.type) {
                DiffLineType.ADDED -> "+".padStart(maxDigits)
                DiffLineType.REMOVED -> "-".padStart(maxDigits)
                DiffLineType.UNCHANGED -> (line.lineNumber?.toString() ?: "").padStart(maxDigits)
            }

            pushStyle(SpanStyle(color = color))
            append(label)
            pop()
        }
    }
}
