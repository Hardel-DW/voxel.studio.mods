package fr.hardel.asset_editor.client.compose.window.chrome

import androidx.compose.ui.awt.ComposePanel
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JFrame

/**
 * Per-OS window chrome strategy.
 *
 * Windows: FlatLaf native decorations (Snap Layouts, aero snap, DWM dark/corners).
 * macOS:   apple.awt.* root pane properties + native traffic lights, manual drag below the native bar.
 * Linux:   undecorated JFrame + in-process drag/resize/snap (historical behavior).
 */
interface NativeWindowChrome {

    /**
     * Caption-area registry consumed by the OS hit-test callback.
     * Only populated by the Compose layer on platforms where [nativeDragHandled] is true.
     */
    val captionRegions: CaptionRegions

    /**
     * True iff the platform handles window drag/resize/maximize-on-double-click itself
     * once [applyTo] has run (Windows via FlatLaf). False for platforms where the Compose
     * drag modifier must drive [beginDrag] / [performDrag] / [endDrag] manually.
     */
    val nativeDragHandled: Boolean

    /** Apply chrome settings to a freshly created, not-yet-visible frame. */
    fun applyTo(frame: JFrame)

    /** Called after the frame becomes visible — lets the impl apply post-show native attributes (DWM). */
    fun onFrameShown(frame: JFrame)

    /**
     * Wire hit-test on a plain Swing panel (splash phase).
     * The panel supplies its own caption predicate, which is called on the AWT-Windows thread —
     * it must be lock-free and side-effect free.
     */
    fun attachSwingContent(panel: JComponent, captionHitTest: (Point) -> Boolean)

    /** Wire hit-test on the ComposePanel (editor phase). Reads [captionRegions]. */
    fun attachComposeContent(panel: ComposePanel)

    /** Manual drag lifecycle. No-op when [nativeDragHandled] is true. */
    fun beginDrag()
    fun performDrag()
    fun endDrag()

    /** Toggle maximize/restore. */
    fun toggleMaximize()

    /** Release resources; called when the frame is disposed. */
    fun dispose()
}
