package fr.hardel.asset_editor.client.compose.lib.highlight

import java.util.ArrayList
import java.util.IdentityHashMap
import java.util.LinkedHashSet
import java.util.Objects

class Highlight {

    private val ranges = LinkedHashSet<HighlightRange>()
    private val containingRegistries = IdentityHashMap<HighlightRegistry, Int>()
    private var priority = 0
    private var type = "highlight"

    fun add(start: Int, end: Int): Highlight =
        add(HighlightRange(start, end))

    fun add(range: HighlightRange): Highlight {
        Objects.requireNonNull(range, "range")
        if (ranges.add(range)) {
            scheduleRepaintInContainingRegistries()
        }
        return this
    }

    fun clear() {
        if (ranges.isEmpty()) {
            return
        }
        ranges.clear()
        scheduleRepaintInContainingRegistries()
    }

    fun contains(range: HighlightRange): Boolean = ranges.contains(range)

    fun size(): Int = ranges.size

    fun priority(): Int = priority

    fun type(): String = type

    fun ranges(): List<HighlightRange> = ranges.toList()

    internal fun registerIn(registry: HighlightRegistry) {
        containingRegistries[registry] = (containingRegistries[registry] ?: 0) + 1
    }

    internal fun deregisterFrom(registry: HighlightRegistry) {
        val count = containingRegistries[registry] ?: return
        if (count <= 1) {
            containingRegistries.remove(registry)
        } else {
            containingRegistries[registry] = count - 1
        }
    }

    private fun scheduleRepaintInContainingRegistries() {
        for (entry in ArrayList(containingRegistries.entries)) {
            if (entry.value > 0) {
                entry.key.scheduleRepaint()
            }
        }
    }
}
