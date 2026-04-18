package fr.hardel.asset_editor.client.compose.lib.highlight

import java.util.LinkedHashMap
import java.util.Objects
import java.util.concurrent.CopyOnWriteArrayList

class HighlightPalette {

    private val styles = LinkedHashMap<String, HighlightStyle>()
    private val listeners = CopyOnWriteArrayList<Runnable>()

    fun set(name: String, style: HighlightStyle): HighlightPalette {
        Objects.requireNonNull(name, "name")
        Objects.requireNonNull(style, "style")
        styles[name] = style
        notifyListeners()
        return this
    }

    fun get(name: String): HighlightStyle? = styles[name]

    fun contains(name: String): Boolean = styles.containsKey(name)

    fun clear() {
        if (styles.isEmpty()) {
            return
        }
        styles.clear()
        notifyListeners()
    }

    fun snapshot(): Map<String, HighlightStyle> = styles.toMap()

    fun addListener(listener: Runnable) {
        listeners.add(listener)
    }

    fun removeListener(listener: Runnable) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach(Runnable::run)
    }
}
