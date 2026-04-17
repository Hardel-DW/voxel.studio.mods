package fr.hardel.asset_editor.client.compose.window.chrome

import androidx.compose.ui.awt.ComposePanel
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatDarkLaf
import java.awt.Container
import java.awt.Frame
import java.awt.Point
import java.awt.event.HierarchyEvent
import java.util.function.Function
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Windows 11 chrome: FlatLaf native decorations + DWM attributes for dark mode and rounded corners.
 *
 * Drag, resize, snap-layouts flyout, aero snap, double-click maximize are all handled by FlatLaf
 * through WM_NCHITTEST once [applyTo] has run. Compose registers draggable rectangles into
 * [captionRegions]; the FlatLaf client-property hook reads them on the AWT-Windows thread.
 */
class WindowsChrome : NativeWindowChrome {

    override val captionRegions = CaptionRegions()
    override val nativeDragHandled: Boolean = true
    override val showComposeWindowControls: Boolean = false
    override val headerStartReservationDp: Int = 0
    override val headerEndReservationDp: Int = NATIVE_CAPTION_BUTTONS_WIDTH_DP

    private var frame: JFrame? = null
    private val composeHitTest = Function<Point, Boolean> { point -> captionRegions.isCaptionAt(point.x, point.y) }

    override fun applyTo(frame: JFrame) {
        this.frame = frame
        frame.isUndecorated = false
        frame.rootPane.apply {
            putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, true)
            putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true)
            putClientProperty(FlatClientProperties.TITLE_BAR_HEIGHT, TITLE_BAR_HEIGHT)
            putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false)
            putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICON, false)
        }
    }

    override fun onFrameShown(frame: JFrame) {
        DwmApi.setDarkMode(frame, true)
        DwmApi.setCornerPreference(frame, DwmApi.CORNER_ROUND)
    }

    override fun attachSwingContent(panel: JComponent, captionHitTest: (Point) -> Boolean) {
        val delegate = Function<Point, Boolean> { point -> captionHitTest(point) }
        panel.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, delegate)
    }

    override fun attachComposeContent(panel: ComposePanel) {
        propagateCaptionHitTest(panel)
        val relevantFlags = (
                HierarchyEvent.DISPLAYABILITY_CHANGED or
                        HierarchyEvent.PARENT_CHANGED or
                        HierarchyEvent.SHOWING_CHANGED
                ).toLong()
        panel.addHierarchyListener { event ->
            if (event.changeFlags and relevantFlags != 0L) {
                SwingUtilities.invokeLater { propagateCaptionHitTest(panel) }
            }
        }
    }

    private fun propagateCaptionHitTest(container: Container) {
        if (container is JComponent) {
            container.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, composeHitTest)
        }
        for (child in container.components) {
            if (child is Container) propagateCaptionHitTest(child)
        }
    }

    override fun beginDrag() = Unit
    override fun performDrag() = Unit
    override fun endDrag() = Unit

    override fun toggleMaximize() {
        val current = frame ?: return
        current.extendedState =
            if (current.extendedState and Frame.MAXIMIZED_BOTH != 0) Frame.NORMAL else Frame.MAXIMIZED_BOTH
    }

    override fun dispose() {
        captionRegions.clear()
        frame = null
    }

    companion object {
        private const val TITLE_BAR_HEIGHT = 48
        private const val NATIVE_CAPTION_BUTTONS_WIDTH_DP = 144

        fun prepareRuntime() {
            System.setProperty("flatlaf.useWindowDecorations", "true")
            UIManager.setLookAndFeel(FlatDarkLaf())
        }
    }
}
