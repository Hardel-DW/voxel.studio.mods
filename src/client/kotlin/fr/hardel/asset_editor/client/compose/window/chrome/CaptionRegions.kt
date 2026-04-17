package fr.hardel.asset_editor.client.compose.window.chrome

import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Lock-free registry of draggable "caption" rectangles, in ComposePanel-local pixel coordinates.
 *
 * Compose writes from its layout phase via [register] / [unregister].
 * The native hit-test callback reads from [isCaptionAt] on the AWT-Windows thread — must never block.
 */
class CaptionRegions {

    private val ref = AtomicReference<PersistentMap<String, Rectangle>>(persistentMapOf())

    fun register(id: String, bounds: Rectangle) {
        ref.updateAndGet { it.put(id, bounds) }
    }

    fun unregister(id: String) {
        ref.updateAndGet { it.remove(id) }
    }

    fun clear() {
        ref.set(persistentMapOf())
    }

    fun isCaptionAt(x: Int, y: Int): Boolean {
        for (bounds in ref.get().values) {
            if (bounds.contains(x, y)) return true
        }
        return false
    }
}
