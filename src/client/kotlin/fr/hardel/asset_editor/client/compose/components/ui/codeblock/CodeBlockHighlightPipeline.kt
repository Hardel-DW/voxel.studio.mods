package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import fr.hardel.asset_editor.client.compose.lib.highlight.Highlight
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRange
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry

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
    if (text.isEmpty()) return emptyList()

    val entries = highlights.entries()
    val ranges = ArrayList<ForegroundHighlightRange>()
    for ((order, entry) in entries.withIndex()) {
        val style = palette.get(entry.name) ?: continue
        val foreground = style.foreground() ?: continue
        for (range in entry.highlight.ranges()) {
            val clamped = range.clampToLength(text.length)
            if (clamped.isCollapsed()) continue
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
        if (style.background() == null && style.underline() == null) return@mapNotNull null
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
    if (text.isEmpty()) return@buildAnnotatedString

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
        if (end <= start) continue

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
        for (range in entry.highlight.ranges()) {
            val clamped = range.clampToLength(text.length)
            if (clamped.isCollapsed()) continue
            val path = layoutResult.getPathForRange(clamped.start(), clamped.end())
            drawPath(path = path, color = background)
        }
    }
}

internal fun DrawScope.drawHighlightUnderlines(
    text: String,
    layoutResult: TextLayoutResult,
    entries: List<PaintHighlightEntry>
) {
    for (entry in entries) {
        val underline = entry.underline ?: continue
        for (range in entry.highlight.ranges()) {
            val clamped = range.clampToLength(text.length)
            if (clamped.isCollapsed()) continue
            for (segment in underlineSegments(layoutResult, clamped)) {
                drawRect(
                    color = underline,
                    topLeft = Offset(segment.left, segment.bottom - segment.height),
                    size = Size(segment.right - segment.left, segment.height)
                )
            }
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
        if (box.width <= 0f) continue

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

/**
 * Slices a list of highlights covering the full text into entries containing only
 * the ranges intersecting `[chunkStart, chunkEnd)`, with offsets re-based to 0.
 * Empty entries are dropped so chunks with no highlights skip drawing entirely.
 */
internal fun sliceHighlightsToChunk(
    entries: List<PaintHighlightEntry>,
    chunkStart: Int,
    chunkEnd: Int
): List<PaintHighlightEntry> {
    if (entries.isEmpty() || chunkEnd <= chunkStart) return emptyList()
    val result = ArrayList<PaintHighlightEntry>(entries.size)
    for (entry in entries) {
        val sliced = Highlight()
        for (range in entry.highlight.ranges()) {
            val s = maxOf(range.start(), chunkStart)
            val e = minOf(range.end(), chunkEnd)
            if (e > s) sliced.add(s - chunkStart, e - chunkStart)
        }
        if (sliced.size() > 0) {
            result += PaintHighlightEntry(sliced, entry.background, entry.underline)
        }
    }
    return result
}
