package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Builds a fully prepared [CodeBlockState] for [text] on a background
 * dispatcher: tokenization (`highlighter.apply`) and the global
 * AnnotatedString are computed off the main thread, leaving only cheap
 * Compose state writes for the UI to absorb on first render.
 *
 * Use this from a `LaunchedEffect` / coroutine when loading large documents
 * (think tens of thousands of lines) where the synchronous `text =` setter
 * would otherwise freeze the UI for hundreds of milliseconds.
 */
suspend fun prepareCodeBlockAsync(
    text: String,
    configure: CodeBlockState.() -> Unit
): CodeBlockState = withContext(Dispatchers.Default) {
    val state = CodeBlockState().apply(configure)
    state.text = text
    val ranges = buildForegroundHighlightRanges(text, state.highlights, state.palette)
    state.precomputedAnnotated = buildHighlightedText(text, ranges, state.textFill)
    state
}
