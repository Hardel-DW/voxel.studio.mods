package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.PointerInputScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
internal val SELECTION_BG = Color(0xFF3B82F6).copy(alpha = 0.35f)
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
    val lineNumberText = remember(visibleLineCount, state.showLineNumbers) {
        if (!state.showLineNumbers) null
        else buildLineNumberText(visibleLineCount)
    }

    var selection by remember(text) { mutableStateOf(TextRange.Zero) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val tapState = remember { TapState() }
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val paddingPx = remember(density) { with(density) { CODE_BLOCK_CONTENT_PADDING.toPx() } }

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
                    val viewportContentWidth = maxWidth
                    Box(
                        modifier = if (state.wrapText) Modifier.fillMaxWidth()
                        else Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = viewportContentWidth)
                                .heightIn(min = viewportHeight)
                                .pointerHoverIcon(PointerIcon.Text)
                                .focusRequester(focusRequester)
                                .focusable()
                                .onKeyEvent { event ->
                                    handleKeyEvent(event, text, selection, clipboard) { newSelection ->
                                        selection = newSelection
                                    }
                                }
                                .pointerInput(text) {
                                    handleCodeSelectionGestures(
                                        text = text,
                                        paddingPx = paddingPx,
                                        tapState = tapState,
                                        layoutProvider = { layoutResult },
                                        onSelectionChange = { selection = it },
                                        onRequestFocus = { runCatching { focusRequester.requestFocus() } }
                                    )
                                }
                                .drawWithContent {
                                    val layout = layoutResult
                                    if (layout != null) {
                                        translate(left = paddingPx, top = paddingPx) {
                                            drawHighlightBackgrounds(text, layout, paintEntries)
                                        }
                                        drawSelectionRectangles(layout, selection, text.length, paddingPx)
                                    }
                                    drawContent()
                                    if (layout != null) {
                                        translate(left = paddingPx, top = paddingPx) {
                                            drawHighlightUnderlines(text, layout, paintEntries)
                                        }
                                    }
                                }
                        ) {
                            BasicText(
                                text = highlightedAnnotated,
                                style = resolvedTextStyle,
                                softWrap = state.wrapText,
                                onTextLayout = { layoutResult = it },
                                modifier = Modifier.padding(CODE_BLOCK_CONTENT_PADDING)
                            )
                        }
                    }
                }
            }
        }
    }
}

internal class TapState(
    var count: Int = 0,
    var lastTime: Long = 0L,
    var lastPos: Offset = Offset.Zero
)

private const val MULTI_TAP_TIMEOUT_MS = 350L
private const val MULTI_TAP_DISTANCE_THRESHOLD_PX_SQ = 100f

internal suspend fun PointerInputScope.handleCodeSelectionGestures(
    text: String,
    paddingPx: Float,
    tapState: TapState,
    layoutProvider: () -> TextLayoutResult?,
    onSelectionChange: (TextRange) -> Unit,
    onRequestFocus: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val initLayout = layoutProvider() ?: return@awaitEachGesture

        val now = down.uptimeMillis
        val withinTime = (now - tapState.lastTime) < MULTI_TAP_TIMEOUT_MS
        val deltaSquared = (down.position - tapState.lastPos).getDistanceSquared()
        val withinDistance = deltaSquared < MULTI_TAP_DISTANCE_THRESHOLD_PX_SQ
        tapState.count = when {
            !withinTime || !withinDistance -> 1
            tapState.count >= 3 -> 1
            else -> tapState.count + 1
        }
        tapState.lastTime = now
        tapState.lastPos = down.position

        val downPosTextSpace = down.position - Offset(paddingPx, paddingPx)
        val downOffset = initLayout.getOffsetForPosition(downPosTextSpace)
        val initialSelection = when (tapState.count) {
            2 -> wordRangeAt(text, downOffset)
            3 -> lineRangeAt(text, downOffset)
            else -> TextRange(downOffset, downOffset)
        }
        onSelectionChange(initialSelection)
        onRequestFocus()
        down.consume()

        val isSingleTap = tapState.count == 1
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                change.consume()
                break
            }
            if (isSingleTap) {
                val layout = layoutProvider()
                if (layout != null) {
                    val movePos = change.position - Offset(paddingPx, paddingPx)
                    val moveOffset = layout.getOffsetForPosition(movePos)
                    onSelectionChange(TextRange(downOffset, moveOffset))
                }
            }
            change.consume()
        }
    }
}

internal fun wordRangeAt(text: String, offset: Int): TextRange {
    if (text.isEmpty()) return TextRange.Zero
    val safeOffset = offset.coerceIn(0, text.length)
    val pos = if (safeOffset >= text.length) safeOffset - 1 else safeOffset
    if (pos < 0) return TextRange(0, 0)

    val char = text[pos]
    return when {
        isWordChar(char) -> {
            var start = pos
            while (start > 0 && isWordChar(text[start - 1])) start--
            var end = pos + 1
            while (end < text.length && isWordChar(text[end])) end++
            TextRange(start, end)
        }
        char.isWhitespace() && char != '\n' -> {
            var start = pos
            while (start > 0 && text[start - 1].isWhitespace() && text[start - 1] != '\n') start--
            var end = pos + 1
            while (end < text.length && text[end].isWhitespace() && text[end] != '\n') end++
            TextRange(start, end)
        }
        else -> TextRange(pos, pos + 1)
    }
}

private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

internal fun lineRangeAt(text: String, offset: Int): TextRange {
    if (text.isEmpty()) return TextRange.Zero
    val safeOffset = offset.coerceIn(0, text.length)

    var start = safeOffset
    while (start > 0 && text[start - 1] != '\n') start--

    var end = safeOffset
    while (end < text.length && text[end] != '\n') end++
    if (end < text.length) end++

    return TextRange(start, end)
}

internal fun handleKeyEvent(
    event: androidx.compose.ui.input.key.KeyEvent,
    text: String,
    selection: TextRange,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    setSelection: (TextRange) -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when {
        event.isCtrlPressed && event.key == Key.C -> {
            if (!selection.collapsed) {
                val s = minOf(selection.start, selection.end).coerceIn(0, text.length)
                val e = maxOf(selection.start, selection.end).coerceIn(0, text.length)
                clipboard.setText(AnnotatedString(text.substring(s, e)))
            }
            true
        }
        event.isCtrlPressed && event.key == Key.A -> {
            setSelection(TextRange(0, text.length))
            true
        }
        else -> false
    }
}

internal fun DrawScope.drawSelectionRectangles(
    layout: TextLayoutResult,
    selection: TextRange,
    textLength: Int,
    paddingPx: Float
) {
    if (selection.collapsed) return
    val s = minOf(selection.start, selection.end).coerceIn(0, textLength)
    val e = maxOf(selection.start, selection.end).coerceIn(0, textLength)
    if (e <= s) return

    val firstLine = layout.getLineForOffset(s)
    val lastLine = layout.getLineForOffset(e)

    for (line in firstLine..lastLine) {
        val top = paddingPx + layout.getLineTop(line)
        val bottom = paddingPx + layout.getLineBottom(line)

        val left = if (line == firstLine) {
            paddingPx + layout.getHorizontalPosition(s, usePrimaryDirection = true)
        } else {
            paddingPx + layout.getLineLeft(line)
        }

        val right = if (line == lastLine) {
            paddingPx + layout.getHorizontalPosition(e, usePrimaryDirection = true)
        } else {
            paddingPx + layout.getLineRight(line)
        }

        if (right > left) {
            drawRect(
                color = SELECTION_BG,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top)
            )
        }
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
