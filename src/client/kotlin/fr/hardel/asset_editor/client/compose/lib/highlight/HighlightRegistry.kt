package fr.hardel.asset_editor.client.compose.lib.highlight

import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Objects
import java.util.concurrent.CopyOnWriteArrayList

class HighlightRegistry {

    data class Entry(val name: String, val highlight: Highlight)

    private val highlights = LinkedHashMap<String, Highlight>()
    private val listeners = CopyOnWriteArrayList<Runnable>()

    private var cachedEntries: List<Entry>? = null
    private var cachedPaintOrder: List<Entry>? = null

    fun set(name: String, highlight: Highlight): HighlightRegistry {
        Objects.requireNonNull(name, "name")
        Objects.requireNonNull(highlight, "highlight")

        val existing = highlights.remove(name)
        existing?.deregisterFrom(this)

        highlights[name] = highlight
        highlight.registerIn(this)
        invalidateCaches()
        scheduleRepaint()
        return this
    }

    fun get(name: String): Highlight? = highlights[name]

    fun contains(name: String): Boolean = highlights.containsKey(name)

    fun clear() {
        if (highlights.isEmpty()) {
            return
        }

        for (highlight in highlights.values) {
            highlight.deregisterFrom(this)
        }

        highlights.clear()
        invalidateCaches()
        scheduleRepaint()
    }

    fun size(): Int = highlights.size

    fun entries(): List<Entry> {
        cachedEntries?.let { return it }
        val snapshot = ArrayList<Entry>(highlights.size)
        for (entry in highlights.entries) {
            snapshot += Entry(entry.key, entry.value)
        }
        val immutable = java.util.Collections.unmodifiableList(snapshot)
        cachedEntries = immutable
        return immutable
    }

    fun entriesInPaintOrder(): List<Entry> {
        cachedPaintOrder?.let { return it }
        val sorted = entries().sortedBy { it.highlight.priority() }
        cachedPaintOrder = sorted
        return sorted
    }

    fun compareOverlayStackingPosition(
        name1: String,
        highlight1: Highlight,
        name2: String,
        highlight2: Highlight
    ): Int {
        if (name1 == name2) {
            return 0
        }

        if (highlight1.priority() != highlight2.priority()) {
            return highlight1.priority().compareTo(highlight2.priority())
        }

        for (entry in highlights.entries) {
            if (entry.key == name1 && entry.value === highlight1) {
                return -1
            }
            if (entry.key == name2 && entry.value === highlight2) {
                return 1
            }
        }
        return 0
    }

    fun addListener(listener: Runnable) {
        listeners.add(listener)
    }

    fun removeListener(listener: Runnable) {
        listeners.remove(listener)
    }

    private fun invalidateCaches() {
        cachedEntries = null
        cachedPaintOrder = null
    }

    internal fun scheduleRepaint() {
        invalidateCaches()
        listeners.forEach(Runnable::run)
    }
}
