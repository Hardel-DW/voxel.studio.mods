package fr.hardel.asset_editor.client.compose.lib.shortcut

import java.awt.event.KeyEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * App-wide AWT key dispatcher. Plugged into the underlying Swing ComposePanel
 * (see [fr.hardel.asset_editor.client.compose.window.VoxelStudioWindow]) so it
 * fires for every key press, regardless of where Compose focus currently lives
 * (inside a popup, a button that just got recomposed away, no focus owner at
 * all, …). This is the only way to get reliable Ctrl+Z, Ctrl+Y, Ctrl+W in a
 * panel that has many transient focusables.
 *
 * Handlers are registered LIFO: the most recently registered handler sees the
 * event first, which gives the natural "innermost screen wins" behaviour when
 * multiple pages register their own shortcuts simultaneously.
 */
object StudioShortcutBus {

    fun interface Handler {
        /** Returns `true` if the event was consumed and should not propagate further. */
        fun handle(event: KeyEvent): Boolean
    }

    private val handlers = CopyOnWriteArrayList<Handler>()

    fun register(handler: Handler): AutoCloseable {
        handlers.add(handler)
        return AutoCloseable { handlers.remove(handler) }
    }

    fun dispatch(event: KeyEvent): Boolean {
        for (i in handlers.indices.reversed()) {
            if (handlers[i].handle(event)) return true
        }
        return false
    }
}
