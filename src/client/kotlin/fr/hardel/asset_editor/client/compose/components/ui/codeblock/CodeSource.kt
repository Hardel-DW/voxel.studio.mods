package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.ui.graphics.Color

/**
 * Per-line decoration: a gutter label (line number, "+", "-", …) and an optional
 * row background. Built once when the source text changes, then sliced cheaply
 * per chunk.
 */
internal data class CodeLineMarker(
    val gutterLabel: String,
    val gutterColor: Color,
    val lineBackground: Color? = null,
    val gutterBackground: Color? = null
)

/**
 * Immutable, precomputed view of the source text used by the chunked renderer.
 * Built once per `text` change so chunks can do O(1) lookups instead of scanning
 * the whole string on every recomposition.
 *
 * - [lineStarts] holds the char offset of the start of each line (size = [lineCount]).
 * - [chunkCharStarts] / [chunkCharEnds] are the inclusive/exclusive global char
 *   bounds of each chunk; the end excludes the newline that terminates the
 *   chunk's last line so text.subSequence yields a slice without a trailing `\n`.
 */
internal class CodeSource(
    val text: String,
    val markers: List<CodeLineMarker>,
    val chunkSize: Int = DEFAULT_CHUNK_SIZE
) {
    val lineCount: Int
    val lineStarts: IntArray
    val chunkCount: Int
    val chunkCharStarts: IntArray
    val chunkCharEnds: IntArray
    val maxLineLength: Int

    init {
        val starts = IntArray(text.length + 1)
        starts[0] = 0
        var count = 1
        var maxLen = 0
        var lineStart = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                if (i - lineStart > maxLen) maxLen = i - lineStart
                lineStart = i + 1
                starts[count++] = lineStart
            }
        }
        if (text.length - lineStart > maxLen) maxLen = text.length - lineStart
        lineCount = count
        lineStarts = starts.copyOf(count)
        maxLineLength = maxLen

        chunkCount = if (lineCount == 0) 0 else (lineCount + chunkSize - 1) / chunkSize
        chunkCharStarts = IntArray(chunkCount)
        chunkCharEnds = IntArray(chunkCount)
        for (c in 0 until chunkCount) {
            val firstLine = c * chunkSize
            val lastLineExclusive = minOf(lineCount, (c + 1) * chunkSize)
            chunkCharStarts[c] = lineStarts[firstLine]
            chunkCharEnds[c] = if (lastLineExclusive < lineCount) {
                lineStarts[lastLineExclusive] - 1
            } else {
                text.length
            }
        }
    }

    fun chunkLineStart(chunkIndex: Int): Int = chunkIndex * chunkSize
    fun chunkLineEnd(chunkIndex: Int): Int = minOf(lineCount, (chunkIndex + 1) * chunkSize)

    companion object {
        const val DEFAULT_CHUNK_SIZE = 64
    }
}
