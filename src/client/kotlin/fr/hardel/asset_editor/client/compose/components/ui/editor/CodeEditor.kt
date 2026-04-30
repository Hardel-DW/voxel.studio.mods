package fr.hardel.asset_editor.client.compose.components.ui.editor

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CODE_LINE_NUMBER_COLOR
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeChunkView
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeLineMarker
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeSource
import kotlinx.coroutines.launch

/**
 * Single-caret JSON editor sitting on top of [CodeChunkView].
 *
 * The editor owns its [EditableLineBuffer] and a per-line [LineHighlightCache];
 * each visible chunk's `AnnotatedString` is assembled on demand by joining the
 * cached annotated lines, so editing one line only invalidates the chunk that
 * contains it. Caret position is the collapsed end of `state.selection`; the
 * caret blink is driven by an `InfiniteTransition` whose alpha is read inside
 * the chunk's draw scope (no recomposition per blink tick).
 *
 * Auto-scroll: a `LaunchedEffect` keyed on the caret offset calls
 * [scrollCaretIntoView] which interpolates the caret's pixel position inside
 * the visible chunk and `animateScrollBy`s only when the caret leaves the
 * viewport's two-line breathing margin.
 *
 * Focus is intentionally **not** requested on mount: doing so inside a parent
 * `verticalScroll` triggers a "bring into view" jump that yanks the host page
 * down to the editor on first render. The user clicks to take focus.
 */
@Composable
fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier = Modifier
) {
    val docVersion = state.documentVersion
    val coroutineScope = rememberCoroutineScope()

    val markers = remember(docVersion) { buildLineNumberMarkers(state.lineCount) }
    val source = remember(docVersion, markers) { CodeSource(state.fullText(), markers) }

    val chunkAnnotated = remember(state, source, docVersion) {
        { chunkIndex: Int -> assembleChunkAnnotated(state, source, chunkIndex) }
    }
    val chunkVersion = remember(state, source, docVersion) {
        { chunkIndex: Int -> hashChunkLines(state, source, chunkIndex) }
    }

    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    val blink = rememberInfiniteTransition(label = "caret-blink")
    val caretAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1f at 0
                1f at 550
                0f at 551
                0f at 1099
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "caret-alpha"
    )

    LaunchedEffect(state.selection.end, source) {
        scrollCaretIntoView(lazyListState, source, state.caretLine(), state.selection.end)
    }

    CodeChunkView(
        source = source,
        chunkAnnotated = chunkAnnotated,
        chunkVersion = chunkVersion,
        paintEntries = emptyList(),
        textStyle = state.textStyle,
        showGutter = true,
        backgroundFill = state.backgroundFill,
        borderFill = state.borderFill,
        minHeight = state.minHeight,
        selection = state.selection,
        onSelectionChange = { state.selection = it },
        lazyListState = lazyListState,
        horizontalScrollState = horizontalScroll,
        focusRequester = focusRequester,
        caretOffset = state.selection.end,
        caretAlphaProvider = { if (focused) caretAlpha else 0f },
        onPreviewKeyEvent = { event ->
            handleEditorKeyEvent(
                event = event,
                state = state,
                requestScrollToCaret = {
                    coroutineScope.launch {
                        scrollCaretIntoView(lazyListState, source, state.caretLine(), state.selection.end)
                    }
                }
            )
        },
        modifier = modifier.onFocusChanged { focused = it.isFocused }
    )
}

private fun buildLineNumberMarkers(lineCount: Int): List<CodeLineMarker> {
    val safeCount = lineCount.coerceAtLeast(1)
    val maxDigits = safeCount.toString().length
    return List(safeCount) { i ->
        CodeLineMarker(
            gutterLabel = (i + 1).toString().padStart(maxDigits),
            gutterColor = CODE_LINE_NUMBER_COLOR
        )
    }
}

private fun assembleChunkAnnotated(
    state: CodeEditorState,
    source: CodeSource,
    chunkIndex: Int
): AnnotatedString {
    val lineStart = source.chunkLineStart(chunkIndex)
    val lineEnd = source.chunkLineEnd(chunkIndex)
    if (lineEnd <= lineStart) return AnnotatedString("")
    return buildAnnotatedString {
        for (i in lineStart until lineEnd) {
            if (i > lineStart) append('\n')
            append(state.cache.get(state.lineAt(i)))
        }
    }
}

private fun hashChunkLines(
    state: CodeEditorState,
    source: CodeSource,
    chunkIndex: Int
): Long {
    val lineStart = source.chunkLineStart(chunkIndex)
    val lineEnd = source.chunkLineEnd(chunkIndex)
    var h = 1125899906842597L
    for (i in lineStart until lineEnd) {
        h = h * 31 + state.lineAt(i).id
    }
    return h
}

/**
 * Scrolls [lazyListState] just enough to keep the caret line visible inside
 * the viewport, with a two-line breathing margin top and bottom.
 *
 * Strategy:
 *  1. If the caret's chunk isn't composed at all, jump to it via
 *     `animateScrollToItem(chunkIndex)`. The next pass refines the offset.
 *  2. Otherwise, approximate the caret's pixel Y by interpolating inside the
 *     visible chunk: `chunkTop + (lineWithinChunk / linesInChunk) * chunkHeight`.
 *     This relies on the monospace assumption that all lines in a chunk have
 *     the same height — true in practice for our text styles.
 *  3. Compare against the viewport and `animateScrollBy` the minimal delta.
 */
private suspend fun scrollCaretIntoView(
    lazyListState: LazyListState,
    source: CodeSource,
    caretLine: Int,
    @Suppress("UNUSED_PARAMETER") caretOffset: Int
) {
    val chunkIndex = caretLine / source.chunkSize
    val info = lazyListState.layoutInfo
    if (info.viewportSize.height == 0) {
        lazyListState.scrollToItem(chunkIndex)
        return
    }

    val item = info.visibleItemsInfo.firstOrNull { it.index == chunkIndex }
    if (item == null) {
        lazyListState.animateScrollToItem(chunkIndex)
        return
    }

    val lineStart = source.chunkLineStart(chunkIndex)
    val linesInChunk = (source.chunkLineEnd(chunkIndex) - lineStart).coerceAtLeast(1)
    val lineHeight = item.size.toFloat() / linesInChunk
    val lineInChunk = (caretLine - lineStart).coerceAtLeast(0)
    val caretTop = item.offset + lineInChunk * lineHeight
    val caretBottom = caretTop + lineHeight

    val viewportTop = 0f
    val viewportBottom = info.viewportSize.height.toFloat()
    val margin = lineHeight * 2f

    val delta = when {
        caretTop < viewportTop + margin -> caretTop - (viewportTop + margin)
        caretBottom > viewportBottom - margin -> caretBottom - (viewportBottom - margin)
        else -> 0f
    }
    if (delta != 0f) {
        lazyListState.animateScrollBy(delta)
    }
}
