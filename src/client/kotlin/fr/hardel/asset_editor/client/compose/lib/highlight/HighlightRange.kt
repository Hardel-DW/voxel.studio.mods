package fr.hardel.asset_editor.client.compose.lib.highlight

class HighlightRange(
    private val start: Int,
    private val end: Int
) : Comparable<HighlightRange> {

    init {
        require(start >= 0) { "start must be >= 0" }
        require(end >= start) { "end must be >= start" }
    }

    fun start(): Int = start

    fun end(): Int = end

    fun length(): Int = end - start

    fun isCollapsed(): Boolean = start == end

    fun clampToLength(textLength: Int): HighlightRange {
        val clampedStart = start.coerceIn(0, textLength)
        val clampedEnd = end.coerceIn(clampedStart, textLength)
        return HighlightRange(clampedStart, clampedEnd)
    }

    fun intersects(other: HighlightRange): Boolean =
        start < other.end && other.start < end

    override fun compareTo(other: HighlightRange): Int {
        val byStart = start.compareTo(other.start)
        if (byStart != 0) {
            return byStart
        }
        return end.compareTo(other.end)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HighlightRange) return false
        return start == other.start && end == other.end
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }

    override fun toString(): String = "HighlightRange[$start, $end)"
}
