package fr.hardel.asset_editor.client.compose.window.chrome

import androidx.compose.ui.awt.ComposePanel
import fr.hardel.asset_editor.client.compose.window.UndecoratedStageWindow
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities

/**
 * Linux chrome: keeps the undecorated JFrame and re-implements drag, resize, snap in process.
 *
 * No native GTK CSD path exists without JBR, so we preserve the behavior we already had before
 * this refactor. Same semantics as the original UndecoratedStageWindow.FrameHost.
 */
class LinuxChrome : NativeWindowChrome {

    override val captionRegions = CaptionRegions()
    override val nativeDragHandled: Boolean = false
    override val showComposeWindowControls: Boolean = true
    override val headerStartReservationDp: Int = 0
    override val headerEndReservationDp: Int = 0

    private var frame: JFrame? = null
    private var host: FrameHost? = null

    override fun applyTo(frame: JFrame) {
        this.frame = frame
        frame.isUndecorated = true
        host = FrameHost(frame).also(FrameHost::install)
    }

    override fun onFrameShown(frame: JFrame) = Unit
    override fun attachSwingContent(panel: JComponent, captionHitTest: (Point) -> Boolean) = Unit
    override fun attachComposeContent(panel: ComposePanel) = Unit

    override fun beginDrag() {
        host?.beginDrag()
    }

    override fun performDrag() {
        host?.performDrag()
    }

    override fun endDrag() {
        host?.endDrag()
    }

    override fun toggleMaximize() {
        host?.toggleMaximize()
    }

    override fun dispose() {
        host = null
        frame = null
    }

    companion object {
        fun prepareRuntime() = Unit
    }

    private class FrameHost(
        private val frame: JFrame,
        private val resizeMargin: Int = 10,
        private val snapMargin: Int = 5
    ) {

        private var boundsBeforeSnap: Rectangle? = null
        private var resizing = false
        private var dragging = false
        private var suppressNextClick = false
        private var activeZone = ResizeZone.NONE
        private var dragOriginX = 0
        private var dragOriginY = 0
        private var dragFrameX = 0
        private var dragFrameY = 0
        private var dragFrameW = 0
        private var dragFrameH = 0
        private var moveOffsetX = 0
        private var moveOffsetY = 0

        private val isSnapped: Boolean get() = boundsBeforeSnap != null

        fun install() {
            val glass = frame.glassPane
            glass.isVisible = true

            glass.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent) {
                    suppressNextClick = false
                    resizing = false

                    val zone = detectZone(event.point)
                    if (zone != ResizeZone.NONE && !isSnapped) {
                        activeZone = zone
                        resizing = true
                        dragOriginX = event.xOnScreen
                        dragOriginY = event.yOnScreen
                        dragFrameX = frame.x
                        dragFrameY = frame.y
                        dragFrameW = frame.width
                        dragFrameH = frame.height
                        return
                    }

                    activeZone = ResizeZone.NONE
                    redispatchToContent(event)
                }

                override fun mouseReleased(event: MouseEvent) {
                    if (resizing) {
                        activeZone = ResizeZone.NONE
                        resizing = false
                        glass.cursor = Cursor.getDefaultCursor()
                        suppressNextClick = true
                        return
                    }

                    redispatchToContent(event)
                }

                override fun mouseClicked(event: MouseEvent) {
                    if (suppressNextClick) {
                        suppressNextClick = false
                        return
                    }

                    redispatchToContent(event)
                }
            })

            glass.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(event: MouseEvent) {
                    if (resizing) return

                    val zone = if (isSnapped) ResizeZone.NONE else detectZone(event.point)
                    glass.cursor = zone.cursor
                    if (zone == ResizeZone.NONE) {
                        redispatchToContent(event)
                    }
                }

                override fun mouseDragged(event: MouseEvent) {
                    if (resizing) {
                        val dx = event.xOnScreen - dragOriginX
                        val dy = event.yOnScreen - dragOriginY
                        activeZone.apply(frame, dx, dy, dragFrameW, dragFrameH, dragFrameX, dragFrameY)
                        return
                    }

                    redispatchToContent(event)
                }
            })
        }

        fun beginDrag() {
            val pointer = MouseInfo.getPointerInfo()?.location ?: return
            dragging = true
            moveOffsetX = pointer.x - frame.x
            moveOffsetY = pointer.y - frame.y
        }

        fun performDrag() {
            if (!dragging) return
            val pointer = MouseInfo.getPointerInfo()?.location ?: return

            if (isSnapped) {
                val ratio = (pointer.x - frame.x).toDouble() / frame.width.coerceAtLeast(1)
                unsnap()
                moveOffsetX = (frame.width * ratio).toInt()
                moveOffsetY = (pointer.y - frame.y).coerceIn(0, frame.height)
            }

            frame.setLocation(pointer.x - moveOffsetX, pointer.y - moveOffsetY)
        }

        fun endDrag() {
            if (!dragging) return
            val pointer = MouseInfo.getPointerInfo()?.location
            dragging = false
            if (pointer != null) applySnap(pointer.x, pointer.y)
        }

        fun toggleMaximize() {
            if (isSnapped) unsnap() else snapTo(windowScreenBounds())
        }

        private fun snapTo(region: Rectangle) {
            if (boundsBeforeSnap == null) boundsBeforeSnap = Rectangle(frame.bounds)
            frame.bounds = region
        }

        private fun unsnap() {
            val saved = boundsBeforeSnap ?: return
            boundsBeforeSnap = null
            frame.bounds = saved
        }

        private fun applySnap(cursorX: Int, cursorY: Int) {
            if (isSnapped) return

            val bounds = screenBoundsAt(cursorX, cursorY)
            val left = cursorX <= bounds.x + snapMargin
            val right = cursorX >= bounds.x + bounds.width - snapMargin
            val top = cursorY <= bounds.y + snapMargin
            val bottom = cursorY >= bounds.y + bounds.height - snapMargin

            if (!left && !right && !top && !bottom) return

            val halfWidth = bounds.width / 2
            val halfHeight = bounds.height / 2
            val x = bounds.x
            val y = bounds.y

            when {
                top && !left && !right -> snapTo(bounds)
                left && top -> snapTo(Rectangle(x, y, halfWidth, halfHeight))
                right && top -> snapTo(Rectangle(x + halfWidth, y, halfWidth, halfHeight))
                left && bottom -> snapTo(Rectangle(x, y + halfHeight, halfWidth, halfHeight))
                right && bottom -> snapTo(Rectangle(x + halfWidth, y + halfHeight, halfWidth, halfHeight))
                left -> snapTo(Rectangle(x, y, halfWidth, bounds.height))
                right -> snapTo(Rectangle(x + halfWidth, y, halfWidth, bounds.height))
                bottom -> snapTo(Rectangle(x, y + halfHeight, bounds.width, halfHeight))
            }
        }

        private fun windowScreenBounds(): Rectangle {
            val windowBounds = frame.bounds
            val windowRight = windowBounds.x + windowBounds.width
            val windowBottom = windowBounds.y + windowBounds.height
            var bestBounds = UndecoratedStageWindow.visualBounds(
                GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
            )
            var bestArea = 0L

            for (device in GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices) {
                val bounds = UndecoratedStageWindow.visualBounds(device.defaultConfiguration)
                val overlapWidth = (minOf(windowRight, bounds.x + bounds.width) - maxOf(windowBounds.x, bounds.x)).coerceAtLeast(0)
                val overlapHeight = (minOf(windowBottom, bounds.y + bounds.height) - maxOf(windowBounds.y, bounds.y)).coerceAtLeast(0)
                val overlapArea = overlapWidth.toLong() * overlapHeight.toLong()
                if (overlapArea > bestArea) {
                    bestArea = overlapArea
                    bestBounds = bounds
                }
            }

            return bestBounds
        }

        private fun screenBoundsAt(x: Int, y: Int): Rectangle {
            for (device in GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices) {
                val bounds = UndecoratedStageWindow.visualBounds(device.defaultConfiguration)
                if (x >= bounds.x && x < bounds.x + bounds.width && y >= bounds.y && y < bounds.y + bounds.height) {
                    return bounds
                }
            }

            return UndecoratedStageWindow.visualBounds(
                GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
            )
        }

        private fun detectZone(point: Point): ResizeZone {
            val width = frame.width
            val height = frame.height
            val north = point.y <= resizeMargin
            val south = point.y >= height - resizeMargin
            val west = point.x <= resizeMargin
            val east = point.x >= width - resizeMargin

            return when {
                north && west -> ResizeZone.NW
                north && east -> ResizeZone.NE
                south && west -> ResizeZone.SW
                south && east -> ResizeZone.SE
                north -> ResizeZone.N
                south -> ResizeZone.S
                west -> ResizeZone.W
                east -> ResizeZone.E
                else -> ResizeZone.NONE
            }
        }

        private fun redispatchToContent(event: MouseEvent) {
            val glass = frame.glassPane
            val contentPane = frame.contentPane
            val contentPoint = SwingUtilities.convertPoint(glass, event.point, contentPane)
            val target = SwingUtilities.getDeepestComponentAt(contentPane, contentPoint.x, contentPoint.y) ?: return
            val targetPoint = SwingUtilities.convertPoint(contentPane, contentPoint, target)
            target.dispatchEvent(
                MouseEvent(
                    target, event.id, event.`when`, event.modifiersEx,
                    targetPoint.x, targetPoint.y, event.xOnScreen, event.yOnScreen,
                    event.clickCount, event.isPopupTrigger, event.button
                )
            )
        }

        private enum class ResizeZone(val cursor: Cursor) {
            NONE(Cursor.getDefaultCursor()),
            N(Cursor(Cursor.N_RESIZE_CURSOR)),
            S(Cursor(Cursor.S_RESIZE_CURSOR)),
            W(Cursor(Cursor.W_RESIZE_CURSOR)),
            E(Cursor(Cursor.E_RESIZE_CURSOR)),
            NW(Cursor(Cursor.NW_RESIZE_CURSOR)),
            NE(Cursor(Cursor.NE_RESIZE_CURSOR)),
            SW(Cursor(Cursor.SW_RESIZE_CURSOR)),
            SE(Cursor(Cursor.SE_RESIZE_CURSOR));

            fun apply(frame: JFrame, dx: Int, dy: Int, oldWidth: Int, oldHeight: Int, oldX: Int, oldY: Int) {
                val minW = frame.minimumSize.width
                val minH = frame.minimumSize.height

                when (this) {
                    N -> {
                        val h = (oldHeight - dy).coerceAtLeast(minH)
                        frame.setBounds(oldX, oldY + oldHeight - h, oldWidth, h)
                    }
                    S -> frame.setSize(oldWidth, (oldHeight + dy).coerceAtLeast(minH))
                    W -> {
                        val w = (oldWidth - dx).coerceAtLeast(minW)
                        frame.setBounds(oldX + oldWidth - w, oldY, w, oldHeight)
                    }
                    E -> frame.setSize((oldWidth + dx).coerceAtLeast(minW), oldHeight)
                    NW -> { N.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY); W.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY) }
                    NE -> { N.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY); E.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY) }
                    SW -> { S.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY); W.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY) }
                    SE -> { S.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY); E.apply(frame, dx, dy, oldWidth, oldHeight, oldX, oldY) }
                    NONE -> Unit
                }
            }
        }
    }
}
