package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.lib.highlight.Highlight
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRange
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry

private val CODE_BLOCK_SHAPE = RoundedCornerShape(10.dp)
private val DEFAULT_TEXT_STYLE = TextStyle(
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

    var textStyle by mutableStateOf(DEFAULT_TEXT_STYLE)
    var textFill by mutableStateOf(VoxelColors.Zinc300)
    var backgroundFill by mutableStateOf(VoxelColors.Zinc950)
    var borderFill by mutableStateOf(VoxelColors.Zinc800)
    var contentPadding by mutableStateOf(PaddingValues(14.dp))
    var lineSpacing by mutableStateOf(4.sp)
    var wrapText by mutableStateOf(false)
    var minHeight by mutableStateOf(0.dp)

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
    val annotatedText = remember(text, renderVersion, state.textFill) {
        buildVisibleText(text, state.highlights, state.palette, state.textFill)
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val resolvedTextStyle = state.textStyle.merge(
        TextStyle(
            color = state.textFill,
            lineHeight = resolveLineHeight(state.textStyle, state.lineSpacing)
        )
    )

    Box(
        modifier = modifier
            .clip(CODE_BLOCK_SHAPE)
            .background(state.backgroundFill)
            .border(1.dp, state.borderFill, CODE_BLOCK_SHAPE)
            .heightIn(min = state.minHeight)
            .verticalScroll(rememberScrollState())
            .then(if (state.wrapText) Modifier else Modifier.horizontalScroll(rememberScrollState()))
    ) {
        Box(
            modifier = Modifier
                .padding(state.contentPadding)
        ) {
            BasicText(
                text = annotatedText,
                style = resolvedTextStyle,
                softWrap = state.wrapText,
                onTextLayout = { layoutResult = it },
                modifier = Modifier.drawWithContent {
                    val currentLayout = layoutResult
                    if (currentLayout != null) {
                        drawHighlightBackgrounds(
                            text = text,
                            layoutResult = currentLayout,
                            registry = state.highlights,
                            palette = state.palette
                        )
                    }
                    drawContent()
                    val currentLayoutAfterDraw = layoutResult
                    if (currentLayoutAfterDraw != null) {
                        drawHighlightUnderlines(
                            text = text,
                            layoutResult = currentLayoutAfterDraw,
                            registry = state.highlights,
                            palette = state.palette
                        )
                    }
                }
            )
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

private fun buildVisibleText(
    text: String,
    highlights: HighlightRegistry,
    palette: HighlightPalette,
    defaultFill: Color
): AnnotatedString = buildAnnotatedString {
    if (text.isEmpty()) {
        return@buildAnnotatedString
    }

    val boundaries = ArrayList<Int>()
    boundaries += 0
    boundaries += text.length

    for (entry in highlights.entries()) {
        val style = palette.get(entry.name) ?: continue
        if (!style.hasForeground()) {
            continue
        }

        for (range in entry.highlight.ranges()) {
            val clamped = range.clampToLength(text.length)
            if (clamped.isCollapsed()) {
                continue
            }
            boundaries += clamped.start()
            boundaries += clamped.end()
        }
    }

    val uniqueBoundaries = boundaries.distinct().sorted()
    for (index in 0 until uniqueBoundaries.lastIndex) {
        val start = uniqueBoundaries[index]
        val end = uniqueBoundaries[index + 1]
        if (end <= start) {
            continue
        }

        val fill = resolveForegroundFill(start, end, defaultFill, highlights, palette)
        pushStyle(SpanStyle(color = fill))
        append(text.substring(start, end))
        pop()
    }
}

private fun resolveForegroundFill(
    start: Int,
    end: Int,
    defaultFill: Color,
    highlights: HighlightRegistry,
    palette: HighlightPalette
): Color {
    var winner: HighlightRegistry.Entry? = null

    for (entry in highlights.entries()) {
        val style = palette.get(entry.name) ?: continue
        if (!style.hasForeground() || !covers(entry.highlight, start, end)) {
            continue
        }

        if (winner == null) {
            winner = entry
            continue
        }

        val compare = highlights.compareOverlayStackingPosition(
            winner.name,
            winner.highlight,
            entry.name,
            entry.highlight
        )
        if (compare < 0) {
            winner = entry
        }
    }

    if (winner == null) {
        return defaultFill
    }

    return palette.get(winner.name)?.foreground() ?: defaultFill
}

private fun covers(highlight: Highlight, start: Int, end: Int): Boolean {
    for (range in highlight.ranges()) {
        if (range.start() <= start && range.end() >= end) {
            return true
        }
    }
    return false
}

private fun ContentDrawScope.drawHighlightBackgrounds(
    text: String,
    layoutResult: TextLayoutResult,
    registry: HighlightRegistry,
    palette: HighlightPalette
) {
    for (entry in registry.entriesInPaintOrder()) {
        val style = palette.get(entry.name) ?: continue
        val background = style.background() ?: continue
        drawHighlightPath(text, layoutResult, entry.highlight, background)
    }
}

private fun ContentDrawScope.drawHighlightUnderlines(
    text: String,
    layoutResult: TextLayoutResult,
    registry: HighlightRegistry,
    palette: HighlightPalette
) {
    for (entry in registry.entriesInPaintOrder()) {
        val style = palette.get(entry.name) ?: continue
        val underline = style.underline() ?: continue
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
