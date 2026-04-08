package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Virtualized code viewer used by both [CodeBlock] and [CodeDiff].
 *
 * Source text is split into fixed-line chunks ([CodeSource]); a [LazyColumn]
 * composes only the visible chunks. Each chunk owns its own [TextLayoutResult]
 * (registered into chunkLayouts so the parent can hit-test against it).
 *
 * Selection is handled at the container level, not per chunk: a single
 * pointerInput at the top hit-tests against the visible chunks via
 * [LazyListState.layoutInfo]. This means clicks anywhere in the container —
 * including dead space below short documents — find the nearest chunk, and
 * drag selection across chunks works without coordinating between them.
 */
@Composable
internal fun CodeChunkView(
    source: CodeSource,
    chunkAnnotated: (Int) -> AnnotatedString,
    chunkVersion: (Int) -> Long,
    paintEntries: List<PaintHighlightEntry>,
    textStyle: TextStyle,
    showGutter: Boolean,
    backgroundFill: Color,
    borderFill: Color,
    minHeight: Dp,
    selection: TextRange,
    onSelectionChange: (TextRange) -> Unit,
    lazyListState: LazyListState,
    horizontalScrollState: ScrollState,
    focusRequester: FocusRequester,
    caretOffset: Int? = null,
    caretAlphaProvider: () -> Float = { 1f },
    onPreviewKeyEvent: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    val text = source.text
    val density = LocalDensity.current
    val paddingPx = remember(density) { with(density) { CODE_BLOCK_CONTENT_PADDING.toPx() } }
    val coroutineScope = rememberCoroutineScope()

    val tapState = remember(text) { TapState() }
    val chunkLayouts = remember(text) { HashMap<Int, TextLayoutResult>() }

    val measurer = rememberTextMeasurer(cacheSize = 4)
    val measuredCharWidthPx = remember(textStyle, measurer) {
        if (source.maxLineLength == 0) 0f
        else measurer.measure(text = "M", style = textStyle).size.width.toFloat()
    }
    val contentMinWidthDp = remember(measuredCharWidthPx, source.maxLineLength, density) {
        with(density) { (measuredCharWidthPx * source.maxLineLength).toDp() } + (CODE_BLOCK_CONTENT_PADDING * 2)
    }
    val gutterWidthDp = remember(source, textStyle, measuredCharWidthPx, density) {
        if (!showGutter) 0.dp
        else {
            val maxLabelLen = source.markers.maxOfOrNull { it.gutterLabel.length } ?: 0
            with(density) { (measuredCharWidthPx * maxLabelLen).toDp() + 24.dp }
        }
    }

    val keyEventModifier = if (onPreviewKeyEvent != null) {
        Modifier.onPreviewKeyEvent(onPreviewKeyEvent)
    } else {
        Modifier.onKeyEvent { event ->
            handleKeyEvent(event, text, selection, onSelectionChange)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(CODE_BLOCK_SHAPE)
            .background(backgroundFill)
            .border(1.dp, borderFill, CODE_BLOCK_SHAPE)
            .heightIn(min = minHeight)
            .then(keyEventModifier)
            .focusRequester(focusRequester)
            .focusable()
    ) {
        val viewportHeight = maxHeight
        val viewportWidth = maxWidth
        val rowContentMinWidth = maxOf(contentMinWidthDp, viewportWidth - gutterWidthDp)
        val gutterWidthPx = with(density) { gutterWidthDp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerHoverIcon(PointerIcon.Text)
                .pointerInput(lazyListState) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val change = event.changes.firstOrNull() ?: continue
                            val deltaY = change.scrollDelta.y
                            if (deltaY == 0f) continue
                            coroutineScope.launch {
                                lazyListState.scrollBy(deltaY * WHEEL_PIXELS_PER_TICK)
                            }
                            change.consume()
                        }
                    }
                }
                .pointerInput(text) {
                    handleContainerSelection(
                        text = text,
                        source = source,
                        chunkLayouts = chunkLayouts,
                        lazyState = lazyListState,
                        horizontalScrollState = horizontalScrollState,
                        gutterWidthPx = gutterWidthPx,
                        paddingLeftPx = paddingPx,
                        paddingTopPx = paddingPx,
                        tapState = tapState,
                        onSelectionChange = onSelectionChange,
                        onRequestFocus = { runCatching { focusRequester.requestFocus() } }
                    )
                }
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight)
            ) {
                items(
                    count = source.chunkCount,
                    key = { it }
                ) { chunkIndex ->
                    ChunkRow(
                        source = source,
                        chunkIndex = chunkIndex,
                        chunkAnnotated = chunkAnnotated,
                        chunkVersion = chunkVersion,
                        paintEntries = paintEntries,
                        textStyle = textStyle,
                        showGutter = showGutter,
                        gutterWidthDp = gutterWidthDp,
                        contentMinWidthDp = rowContentMinWidth,
                        horizontalScrollState = horizontalScrollState,
                        selection = selection,
                        caretOffset = caretOffset,
                        caretAlphaProvider = caretAlphaProvider,
                        paddingPx = paddingPx,
                        onChunkLayout = { layout -> chunkLayouts[chunkIndex] = layout }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChunkRow(
    source: CodeSource,
    chunkIndex: Int,
    chunkAnnotated: (Int) -> AnnotatedString,
    chunkVersion: (Int) -> Long,
    paintEntries: List<PaintHighlightEntry>,
    textStyle: TextStyle,
    showGutter: Boolean,
    gutterWidthDp: Dp,
    contentMinWidthDp: Dp,
    horizontalScrollState: ScrollState,
    selection: TextRange,
    caretOffset: Int?,
    caretAlphaProvider: () -> Float,
    paddingPx: Float,
    onChunkLayout: (TextLayoutResult) -> Unit
) {
    val charStart = source.chunkCharStarts[chunkIndex]
    val charEnd = source.chunkCharEnds[chunkIndex]
    val lineStart = source.chunkLineStart(chunkIndex)
    val lineEnd = source.chunkLineEnd(chunkIndex)
    val chunkLength = charEnd - charStart
    val isFirstChunk = chunkIndex == 0
    val isLastChunk = chunkIndex == source.chunkCount - 1
    val verticalPadTop = if (isFirstChunk) CODE_BLOCK_CONTENT_PADDING else 0.dp
    val verticalPadBottom = if (isLastChunk) CODE_BLOCK_CONTENT_PADDING else 0.dp

    val versionKey = chunkVersion(chunkIndex)
    val annotated = remember(versionKey, chunkIndex) { chunkAnnotated(chunkIndex) }
    val chunkPaintEntries = remember(paintEntries, charStart, charEnd) {
        sliceHighlightsToChunk(paintEntries, charStart, charEnd)
    }
    val gutterAnnotated = remember(source, lineStart, lineEnd) {
        if (!showGutter) AnnotatedString("") else buildChunkGutter(source.markers, lineStart, lineEnd)
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        if (showGutter) {
            ChunkGutter(
                source = source,
                lineStart = lineStart,
                lineEnd = lineEnd,
                gutterAnnotated = gutterAnnotated,
                textStyle = textStyle,
                width = gutterWidthDp,
                topPad = verticalPadTop,
                bottomPad = verticalPadBottom
            )
        }
        ChunkContent(
            source = source,
            charStart = charStart,
            chunkLength = chunkLength,
            lineStart = lineStart,
            lineEnd = lineEnd,
            annotated = annotated,
            paintEntries = chunkPaintEntries,
            textStyle = textStyle,
            contentMinWidthDp = contentMinWidthDp,
            horizontalScrollState = horizontalScrollState,
            selection = selection,
            caretOffset = caretOffset,
            caretAlphaProvider = caretAlphaProvider,
            paddingPx = paddingPx,
            topPad = verticalPadTop,
            bottomPad = verticalPadBottom,
            onChunkLayout = onChunkLayout,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Read-only convenience wrapper around [CodeChunkView] that creates and owns
 * the selection / focus / scroll / lazy-list state internally so the simple
 * viewer call sites ([CodeBlock], [CodeDiff]) don't have to repeat the same
 * `remember { … }` boilerplate. Builds the static `chunkAnnotated` lambda by
 * slicing [annotated] through [sliceAnnotatedToChunk]; the version key is
 * fixed at `0L` because the source is immutable.
 *
 * Editable callers ([fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditor])
 * still talk to [CodeChunkView] directly so they can hoist selection,
 * provide a caret and supply per-line cached annotated strings.
 */
@Composable
internal fun CodeChunkViewStatic(
    source: CodeSource,
    annotated: AnnotatedString,
    paintEntries: List<PaintHighlightEntry>,
    textStyle: TextStyle,
    showGutter: Boolean,
    backgroundFill: Color,
    borderFill: Color,
    minHeight: Dp,
    modifier: Modifier = Modifier
) {
    val text = source.text
    var selection by remember(text) { mutableStateOf(TextRange.Zero) }
    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()
    val focusRequester = remember(text) { FocusRequester() }
    val chunkAnnotated = remember(annotated, source) {
        { chunkIndex: Int -> sliceAnnotatedToChunk(annotated, source, chunkIndex) }
    }
    val chunkVersion = remember { { _: Int -> 0L } }

    CodeChunkView(
        source = source,
        chunkAnnotated = chunkAnnotated,
        chunkVersion = chunkVersion,
        paintEntries = paintEntries,
        textStyle = textStyle,
        showGutter = showGutter,
        backgroundFill = backgroundFill,
        borderFill = borderFill,
        minHeight = minHeight,
        selection = selection,
        onSelectionChange = { },
        lazyListState = lazyListState,
        horizontalScrollState = horizontalScroll,
        focusRequester = focusRequester,
        modifier = modifier
    )
}

@Composable
private fun ChunkGutter(
    source: CodeSource,
    lineStart: Int,
    lineEnd: Int,
    gutterAnnotated: AnnotatedString,
    textStyle: TextStyle,
    width: Dp,
    topPad: Dp,
    bottomPad: Dp
) {
    var layoutResult by remember(gutterAnnotated) { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        modifier = Modifier
            .widthIn(min = width)
            .drawBehind {
                drawRect(
                    color = CODE_GUTTER_BORDER,
                    topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                    size = Size(1.dp.toPx(), size.height)
                )
                val layout = layoutResult ?: return@drawBehind
                drawPerLineBackgrounds(layout, lineStart, lineEnd, topPad.toPx()) { line ->
                    source.markers[line].gutterBackground
                }
            }
    ) {
        BasicText(
            text = gutterAnnotated,
            style = textStyle,
            onTextLayout = { layoutResult = it },
            modifier = Modifier.padding(
                top = topPad,
                bottom = bottomPad,
                start = 12.dp,
                end = 12.dp
            )
        )
    }
}

@Composable
private fun ChunkContent(
    source: CodeSource,
    charStart: Int,
    chunkLength: Int,
    lineStart: Int,
    lineEnd: Int,
    annotated: AnnotatedString,
    paintEntries: List<PaintHighlightEntry>,
    textStyle: TextStyle,
    contentMinWidthDp: Dp,
    horizontalScrollState: ScrollState,
    selection: TextRange,
    caretOffset: Int?,
    caretAlphaProvider: () -> Float,
    paddingPx: Float,
    topPad: Dp,
    bottomPad: Dp,
    onChunkLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier
) {
    var layoutResult by remember(annotated) { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier.horizontalScroll(horizontalScrollState)) {
        Box(
            modifier = Modifier
                .widthIn(min = contentMinWidthDp)
                .drawWithContent {
                    val layout = layoutResult
                    if (layout != null) {
                        val padTopPx = topPad.toPx()
                        drawPerLineBackgrounds(layout, lineStart, lineEnd, padTopPx) { line ->
                            source.markers[line].lineBackground
                        }
                        translate(left = paddingPx, top = padTopPx) {
                            drawHighlightBackgrounds(annotated.text, layout, paintEntries)
                        }
                        drawSelectionRectangles(
                            layout = layout,
                            selection = selection,
                            chunkStart = charStart,
                            chunkLength = chunkLength,
                            paddingLeftPx = paddingPx,
                            paddingTopPx = padTopPx
                        )
                    }
                    drawContent()
                    if (layout != null) {
                        val padTopPx = topPad.toPx()
                        translate(left = paddingPx, top = padTopPx) {
                            drawHighlightUnderlines(annotated.text, layout, paintEntries)
                        }
                        if (caretOffset != null && caretOffset in charStart..(charStart + chunkLength)) {
                            val alpha = caretAlphaProvider()
                            if (alpha > 0f) {
                                val localOffset = caretOffset - charStart
                                val cursorRect = layout.getCursorRect(localOffset)
                                drawRect(
                                    color = Color.White.copy(alpha = alpha),
                                    topLeft = Offset(paddingPx + cursorRect.left, padTopPx + cursorRect.top),
                                    size = Size(1.5.dp.toPx(), cursorRect.height)
                                )
                            }
                        }
                    }
                }
        ) {
            BasicText(
                text = annotated,
                style = textStyle,
                softWrap = false,
                onTextLayout = {
                    layoutResult = it
                    onChunkLayout(it)
                },
                modifier = Modifier.padding(
                    start = CODE_BLOCK_CONTENT_PADDING,
                    end = CODE_BLOCK_CONTENT_PADDING,
                    top = topPad,
                    bottom = bottomPad
                )
            )
        }
    }
}

private suspend fun PointerInputScope.handleContainerSelection(
    text: String,
    source: CodeSource,
    chunkLayouts: Map<Int, TextLayoutResult>,
    lazyState: LazyListState,
    horizontalScrollState: ScrollState,
    gutterWidthPx: Float,
    paddingLeftPx: Float,
    paddingTopPx: Float,
    tapState: TapState,
    onSelectionChange: (TextRange) -> Unit,
    onRequestFocus: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downGlobal = hitTestToGlobalOffset(
            position = down.position,
            source = source,
            chunkLayouts = chunkLayouts,
            lazyState = lazyState,
            horizontalScrollPx = horizontalScrollState.value.toFloat(),
            gutterWidthPx = gutterWidthPx,
            paddingLeftPx = paddingLeftPx,
            paddingTopPx = paddingTopPx
        ) ?: return@awaitEachGesture

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

        val initialSelection = selectionForTapCount(text, downGlobal, tapState.count)
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
                val moveGlobal = hitTestToGlobalOffset(
                    position = change.position,
                    source = source,
                    chunkLayouts = chunkLayouts,
                    lazyState = lazyState,
                    horizontalScrollPx = horizontalScrollState.value.toFloat(),
                    gutterWidthPx = gutterWidthPx,
                    paddingLeftPx = paddingLeftPx,
                    paddingTopPx = paddingTopPx
                )
                if (moveGlobal != null) {
                    onSelectionChange(TextRange(downGlobal, moveGlobal))
                }
            }
            change.consume()
        }
    }
}

/**
 * Hit-tests a pointer position (in the container's local coords) against the
 * currently visible chunks and returns the corresponding global character offset.
 *
 * Clicks above the first visible chunk snap to its start, clicks below the last
 * visible chunk snap to its end. This makes empty space inside the container —
 * gutter, dead area below short documents, etc. — fully clickable.
 */
private fun hitTestToGlobalOffset(
    position: Offset,
    source: CodeSource,
    chunkLayouts: Map<Int, TextLayoutResult>,
    lazyState: LazyListState,
    horizontalScrollPx: Float,
    gutterWidthPx: Float,
    paddingLeftPx: Float,
    paddingTopPx: Float
): Int? {
    val visible = lazyState.layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return null

    var target = visible.firstOrNull {
        val top = it.offset.toFloat()
        val bottom = (it.offset + it.size).toFloat()
        position.y in top..bottom
    }
    if (target == null) {
        target = if (position.y < visible.first().offset) visible.first() else visible.last()
    }
    val chunkIndex = target.index
    val layout = chunkLayouts[chunkIndex] ?: return null

    val chunkCharStart = source.chunkCharStarts[chunkIndex]
    val chunkLength = source.chunkCharEnds[chunkIndex] - chunkCharStart

    val rowY = position.y - target.offset
    val topPadOffset = if (chunkIndex == 0) paddingTopPx else 0f
    val localY = rowY - topPadOffset
    val localX = position.x + horizontalScrollPx - gutterWidthPx - paddingLeftPx

    val localOffset = layout.getOffsetForPosition(Offset(localX.coerceAtLeast(0f), localY.coerceAtLeast(0f)))
        .coerceIn(0, chunkLength)
    return chunkCharStart + localOffset
}

/**
 * Pixels of vertical scroll per mouse-wheel "notch". Compose Desktop emits a
 * normalised `scrollDelta.y` of ±1 per wheel tick, so we multiply to get a
 * comfortable scroll speed roughly matching what `Modifier.scrollable` would
 * produce on its own.
 */
private const val WHEEL_PIXELS_PER_TICK = 64f

/**
 * Draws a full-width background rectangle for every line in `[lineStart, lineEnd)`
 * whose [colorFor] returns a non-null color, using [layout] to position each line
 * vertically. Shared by [ChunkContent] (line backgrounds for diff additions /
 * removals) and [ChunkGutter] (gutter backgrounds for the same), where the only
 * difference is which marker color is consulted.
 */
private fun DrawScope.drawPerLineBackgrounds(
    layout: TextLayoutResult,
    lineStart: Int,
    lineEnd: Int,
    padTopPx: Float,
    colorFor: (lineIndex: Int) -> Color?
) {
    for (line in lineStart until lineEnd) {
        val bg = colorFor(line) ?: continue
        val localLine = line - lineStart
        val top = padTopPx + layout.getLineTop(localLine)
        val bottom = padTopPx + layout.getLineBottom(localLine)
        drawRect(bg, topLeft = Offset(0f, top), size = Size(size.width, bottom - top))
    }
}

private fun buildChunkGutter(
    markers: List<CodeLineMarker>,
    lineStart: Int,
    lineEnd: Int
): AnnotatedString = buildAnnotatedString {
    for (line in lineStart until lineEnd) {
        if (line > lineStart) append('\n')
        val marker = markers[line]
        pushStyle(SpanStyle(color = marker.gutterColor))
        append(marker.gutterLabel)
        pop()
    }
}
