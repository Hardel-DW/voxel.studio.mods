package fr.hardel.asset_editor.client.compose.window.chrome

import androidx.compose.ui.awt.ComposePanel
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JFrame

/**
 * macOS chrome: native traffic lights + content extended under the (transparent) title bar.
 *
 * FlatLaf native decorations are not supported on macOS, so we rely purely on the AWT client
 * properties exposed by Apple's JDK port. The native title strip handles drag by itself; the
 * rest of our custom header uses [beginDrag]/[performDrag]/[endDrag] to move the frame manually.
 */
class MacOsChrome : NativeWindowChrome {

    override val captionRegions = CaptionRegions()
    override val nativeDragHandled: Boolean = false
    override val showComposeWindowControls: Boolean = false
    override val headerStartReservationDp: Int = TRAFFIC_LIGHTS_WIDTH_DP
    override val headerEndReservationDp: Int = 0

    private var frame: JFrame? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun applyTo(frame: JFrame) {
        this.frame = frame
        frame.isUndecorated = false
        frame.rootPane.apply {
            putClientProperty("apple.awt.fullWindowContent", true)
            putClientProperty("apple.awt.transparentTitleBar", true)
            putClientProperty("apple.awt.windowTitleVisible", false)
        }
    }

    override fun onFrameShown(frame: JFrame) = Unit
    override fun attachSwingContent(panel: JComponent, captionHitTest: (Point) -> Boolean) = Unit
    override fun attachComposeContent(panel: ComposePanel) = Unit

    override fun beginDrag() {
        val current = frame ?: return
        val pointer = MouseInfo.getPointerInfo()?.location ?: return
        dragOffsetX = pointer.x - current.x
        dragOffsetY = pointer.y - current.y
    }

    override fun performDrag() {
        val current = frame ?: return
        val pointer = MouseInfo.getPointerInfo()?.location ?: return
        current.setLocation(pointer.x - dragOffsetX, pointer.y - dragOffsetY)
    }

    override fun endDrag() = Unit

    override fun toggleMaximize() {
        val current = frame ?: return
        current.extendedState =
            if (current.extendedState and Frame.MAXIMIZED_BOTH != 0) Frame.NORMAL else Frame.MAXIMIZED_BOTH
    }

    override fun dispose() {
        frame = null
    }

    companion object {
        // Reserved gutter at the header's leading edge for the macOS traffic-light buttons.
        // Value covers the red/yellow/green cluster plus a small trailing margin.
        private const val TRAFFIC_LIGHTS_WIDTH_DP = 78

        fun prepareRuntime() {
            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
            System.setProperty("apple.laf.useScreenMenuBar", "true")
        }
    }
}
