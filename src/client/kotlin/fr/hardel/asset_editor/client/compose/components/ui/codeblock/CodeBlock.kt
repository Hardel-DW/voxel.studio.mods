package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.lib.highlight.Highlight
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRange
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry

private val CODE_BLOCK_SHAPE = RoundedCornerShape(10.dp)
private val LINE_NUMBER_COLOR = StudioColors.Zinc600
private val LINE_GUTTER_BORDER = StudioColors.Zinc800.copy(alpha = 0.5f)
internal val CODE_BLOCK_CONTENT_PADDING = 16.dp
private val CODE_BLOCK_GUTTER_PADDING = PaddingValues(
    start = 12.dp,
    top = 16.dp,
    end = 12.dp,
    bottom = 16.dp
)

val CODE_TEXT_STYLE = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp
)

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
            if (value == null) {
                highlights.clear()
            } else {
                refreshHighlights()
            }
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
    val visibleLineCount = remember(text) { text.count { it == '\n' } + 1 }
    val foregroundRanges = remember(text, renderVersion) {
        buildForegroundHighlightRanges(text, state.highlights, state.palette)
    }
    val paintEntries = remember(renderVersion) {
        buildPaintEntries(state.highlights, state.palette)
    }
    val highlightedAnnotated = remember(text, foregroundRanges, state.textFill) {
        buildHighlightedText(text, foregroundRanges, state.textFill)
    }
    val transformation = remember(highlightedAnnotated) {
        CachedHighlightTransformation(highlightedAnnotated)
    }
    val lineNumberText = remember(visibleLineCount, state.showLineNumbers) {
        if (!state.showLineNumbers) null
        else buildLineNumberText(visibleLineCount)
    }

    var textFieldValue by remember(text) { mutableStateOf(TextFieldValue(text)) }
    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            val sel = TextRange(
                textFieldValue.selection.start.coerceIn(0, text.length),
                textFieldValue.selection.end.coerceIn(0, text.length)
            )
            textFieldValue = textFieldValue.copy(text = text, selection = sel)
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .clip(CODE_BLOCK_SHAPE)
            .background(state.backgroundFill)
            .border(1.dp, state.borderFill, CODE_BLOCK_SHAPE)
            .heightIn(min = state.minHeight)
    ) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (state.showLineNumbers && lineNumberText != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = viewportHeight)
                            .drawBehind {
                                drawRect(
                                    color = LINE_GUTTER_BORDER,
                                    topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                                    size = Size(1.dp.toPx(), size.height)
                                )
                            }
                    ) {
                        BasicText(
                            text = lineNumberText,
                            style = resolvedTextStyle.copy(color = LINE_NUMBER_COLOR),
                            modifier = Modifier.padding(CODE_BLOCK_GUTTER_PADDING)
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val contentMinWidth = (maxWidth - CODE_BLOCK_CONTENT_PADDING * 2).coerceAtLeast(0.dp)
                    val contentMinHeight = (viewportHeight - CODE_BLOCK_CONTENT_PADDING * 2).coerceAtLeast(0.dp)
                    Box(
                        modifier = Modifier
                            .then(if (state.wrapText) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState()))
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { updated ->
                                textFieldValue = if (updated.text == text) updated else updated.copy(text = text)
                            },
                            readOnly = true,
                            textStyle = resolvedTextStyle,
                            cursorBrush = SolidColor(Color.Transparent),
                            visualTransformation = transformation,
                            onTextLayout = { layoutResult = it },
                            modifier = Modifier
                                .widthIn(min = contentMinWidth)
                                .heightIn(min = contentMinHeight)
                                .padding(CODE_BLOCK_CONTENT_PADDING)
                                .drawWithContent {
                                    val layout = layoutResult ?: run {
                                        drawContent()
                                        return@drawWithContent
                                    }
                                    drawHighlightBackgrounds(text, layout, paintEntries)
                                    drawContent()
                                    drawHighlightUnderlines(text, layout, paintEntries)
                                }
                        )
                    }
                }
            }
        }
    }
}

internal class CachedHighlightTransformation(
    private val cached: AnnotatedString
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == cached.text) {
            return TransformedText(cached, OffsetMapping.Identity)
        }
        return TransformedText(text, OffsetMapping.Identity)
    }
}

private fun buildLineNumberText(lineCount: Int): AnnotatedString {
    val maxDigits = lineCount.toString().length
    return buildAnnotatedString {
        for (i in 1..lineCount) {
            if (i > 1) append("\n")
            append(i.toString().padStart(maxDigits))
        }
    }
}

private fun resolveLineHeight(textStyle: TextStyle, lineSpacing: TextUnit): TextUnit {
    if (textStyle.lineHeight != TextUnit.Unspecified) {
        return textStyle.lineHeight
    }
    if (textStyle.fontSize == TextUnit.Unspecified) {
        return lineSpacing
    }
    return (textStyle.fontSize.value + lineSpacing.value).sp
}

internal data class ForegroundHighlightRange(
    val start: Int,
    val end: Int,
    val color: Color,
    val priority: Int,
    val order: Int
)

internal data class PaintHighlightEntry(
    val highlight: Highlight,
    val background: Color?,
    val underline: Color?
)

internal fun buildForegroundHighlightRanges(
    text: String,
    highlights: HighlightRegistry,
    palette: HighlightPalette
): List<ForegroundHighlightRange> {
    if (text.isEmpty()) {
        return emptyList()
    }

    val entries = highlights.entries()
    val ranges = ArrayList<ForegroundHighlightRange>()
    for ((order, entry) in entries.withIndex()) {
        val style = palette.get(entry.name) ?: continue
        val foreground = style.foreground() ?: continue
        for (range in entry.highlight.ranges()) {
            val clamped = range.clampToLength(text.length)
            if (clamped.isCollapsed()) {
                continue
            }
            ranges += ForegroundHighlightRange(
                start = clamped.start(),
                end = clamped.end(),
                color = foreground,
                priority = entry.highlight.priority(),
                order = order
            )
        }
    }
    return ranges
}

internal fun buildPaintEntries(
    highlights: HighlightRegistry,
    palette: HighlightPalette
): List<PaintHighlightEntry> =
    highlights.entriesInPaintOrder().mapNotNull { entry ->
        val style = palette.get(entry.name) ?: return@mapNotNull null
        if (style.background() == null && style.underline() == null) {
            return@mapNotNull null
        }
        PaintHighlightEntry(
            highlight = entry.highlight,
            background = style.background(),
            underline = style.underline()
        )
    }

internal fun buildHighlightedText(
    text: String,
    foregroundRanges: List<ForegroundHighlightRange>,
    defaultFill: Color
): AnnotatedString = buildAnnotatedString {
    if (text.isEmpty()) {
        return@buildAnnotatedString
    }

    val boundaries = linkedSetOf(0, text.length)
    val rangesByStart = HashMap<Int, MutableList<ForegroundHighlightRange>>()
    val rangesByEnd = HashMap<Int, MutableList<ForegroundHighlightRange>>()

    for (range in foregroundRanges) {
        boundaries += range.start
        boundaries += range.end
        rangesByStart.getOrPut(range.start) { ArrayList() } += range
        rangesByEnd.getOrPut(range.end) { ArrayList() } += range
    }

    val uniqueBoundaries = boundaries.sorted()
    val activeRanges = ArrayList<ForegroundHighlightRange>()
    for (index in 0 until uniqueBoundaries.lastIndex) {
        val start = uniqueBoundaries[index]
        val end = uniqueBoundaries[index + 1]
        if (end <= start) {
            continue
        }

        rangesByEnd[start]?.let(activeRanges::removeAll)
        rangesByStart[start]?.let(activeRanges::addAll)
        val fill = activeRanges.maxWithOrNull(
            compareBy<ForegroundHighlightRange>({ it.priority }, { it.order })
        )?.color ?: defaultFill
        pushStyle(SpanStyle(color = fill))
        append(text.substring(start, end))
        pop()
    }
}

internal fun DrawScope.drawHighlightBackgrounds(
    text: String,
    layoutResult: TextLayoutResult,
    entries: List<PaintHighlightEntry>
) {
    for (entry in entries) {
        val background = entry.background ?: continue
        drawHighlightPath(text, layoutResult, entry.highlight, background)
    }
}

internal fun DrawScope.drawHighlightUnderlines(
    text: String,
    layoutResult: TextLayoutResult,
    entries: List<PaintHighlightEntry>
) {
    for (entry in entries) {
        val underline = entry.underline ?: continue
        drawHighlightUnderline(text, layoutResult, entry.highlight, underline)
    }
}

private fun DrawScope.drawHighlightPath(
    text: String,
    layoutResult: TextLayoutResult,
    highlight: Highlight,
    color: Color
) {
    for (range in highlight.ranges()) {
        val clamped = range.clampToLength(text.length)
        if (clamped.isCollapsed()) {
            continue
        }

        val path = layoutResult.getPathForRange(clamped.start(), clamped.end())
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawHighlightUnderline(
    text: String,
    layoutResult: TextLayoutResult,
    highlight: Highlight,
    color: Color
) {
    for (range in highlight.ranges()) {
        val clamped = range.clampToLength(text.length)
        if (clamped.isCollapsed()) {
            continue
        }

        for (segment in underlineSegments(layoutResult, clamped)) {
            drawRect(
                color = color,
                topLeft = Offset(segment.left, segment.bottom - segment.height),
                size = Size(segment.right - segment.left, segment.height)
            )
        }
    }
}

private data class UnderlineSegment(
    val left: Float,
    val right: Float,
    val bottom: Float,
    val height: Float
)

private fun underlineSegments(
    layoutResult: TextLayoutResult,
    range: HighlightRange
): List<UnderlineSegment> {
    val segments = ArrayList<UnderlineSegment>()
    var currentLine = -1
    var currentLeft = 0f
    var currentRight = 0f
    var currentBottom = 0f
    var currentHeight = 0f

    for (offset in range.start() until range.end()) {
        val box = layoutResult.getBoundingBox(offset)
        if (box.width <= 0f) {
            continue
        }

        val line = layoutResult.getLineForOffset(offset)
        val lineBottom = layoutResult.getLineBottom(line)
        val thickness = (box.height * 0.08f).coerceAtLeast(1f)

        if (line != currentLine) {
            if (currentLine != -1) {
                segments += UnderlineSegment(currentLeft, currentRight, currentBottom, currentHeight)
            }
            currentLine = line
            currentLeft = box.left
            currentRight = box.right
            currentBottom = lineBottom
            currentHeight = thickness
            continue
        }

        currentRight = box.right
        currentBottom = lineBottom
        currentHeight = maxOf(currentHeight, thickness)
    }

    if (currentLine != -1) {
        segments += UnderlineSegment(currentLeft, currentRight, currentBottom, currentHeight)
    }

    return segments
}
