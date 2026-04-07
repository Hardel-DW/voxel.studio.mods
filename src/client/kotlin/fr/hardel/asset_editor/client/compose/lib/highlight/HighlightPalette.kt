package fr.hardel.asset_editor.client.compose.lib.highlight

import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Objects

class HighlightPalette {

    private val styles = LinkedHashMap<String, HighlightStyle>()
    private val listeners = ArrayList<Runnable>()

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
        listeners += listener
    }

    private fun notifyListeners() {
        for (listener in listeners.toList()) {
            listener.run()
        }
    }
}
