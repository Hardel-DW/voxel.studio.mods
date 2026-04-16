package fr.hardel.asset_editor.client.compose.window

import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.AbstractAction
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import org.slf4j.LoggerFactory

open class UndecoratedStageWindow(
    private val minWidth: Int,
    private val minHeight: Int
) {

    private val logger = LoggerFactory.getLogger(UndecoratedStageWindow::class.java)

    protected var frame: JFrame? = null
        private set

    private var frameHost: FrameHost? = null

    protected fun initializeWindow(): Boolean {
        if (frame != null) return true

        if (GraphicsEnvironment.isHeadless()) {
            logger.error(
                "Cannot open window: AWT is headless. java.awt.headless={}",
                System.getProperty("java.awt.headless")
            )
            return false
        }

        val screenBounds = visualBounds(
            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        )
        val width = (screenBounds.width * 0.75).toInt().coerceAtLeast(minWidth)
        val height = (screenBounds.height * 0.75).toInt().coerceAtLeast(minHeight)

        val createdFrame = JFrame().apply {
            isUndecorated = true
            minimumSize = Dimension(minWidth, minHeight)
            setSize(width, height)
            setLocation(
                screenBounds.x + (screenBounds.width - width) / 2,
                screenBounds.y + (screenBounds.height - height) / 2
            )
            background = Color.BLACK
        }

        (createdFrame.contentPane as? JPanel)?.apply {
            background = Color.BLACK
            isOpaque = true
        }

        createdFrame.rootPane.apply {
            val action = "window-hide-on-escape"
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), action)
            actionMap.put(action, object : AbstractAction() {
                override fun actionPerformed(event: ActionEvent?) {
                    hideWindow()
                }
            })
        }

        val createdHost = FrameHost(createdFrame)
        createdHost.install()

        createdFrame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                if (frame === createdFrame) {
                    frame = null
                    frameHost = null
                }
            }

            override fun windowActivated(event: WindowEvent) {
                onWindowFocused()
            }
        })

        frame = createdFrame
        frameHost = createdHost
        return true
    }

    protected open fun onWindowFocused() {}

    protected fun setRoot(root: Component) {
        val currentFrame = frame ?: return
        currentFrame.contentPane.removeAll()
        currentFrame.contentPane.add(root)
        currentFrame.contentPane.revalidate()
        currentFrame.contentPane.repaint()
    }

    protected fun showWindow() {
        val currentFrame = frame ?: return
        if (!currentFrame.isVisible) currentFrame.isVisible = true
        currentFrame.toFront()
        currentFrame.requestFocus()
    }

    protected fun hideWindow() {
        frame?.isVisible = false
    }

    protected fun toggleMaximize() {
        frameHost?.toggleMaximize()
    }

    protected fun minimizeWindow() {
        frame?.state = JFrame.ICONIFIED
    }

    protected fun closeWindow() {
        frame?.isVisible = false
    }

    protected fun beginFrameDrag() {
        frameHost?.beginDrag()
    }

    protected fun performFrameDrag() {
        frameHost?.performDrag()
    }

    protected fun endFrameDrag() {
        frameHost?.endDrag()
    }

    protected fun isFrameSnapped(): Boolean = frameHost?.isSnapped == true

    protected fun frameLocation(): Point? = frame?.location

    protected fun frameSize(): Dimension? = frame?.size

    protected fun bindDragArea(component: Component) {
        frameHost?.installDragArea(component)
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

        val isSnapped: Boolean get() = boundsBeforeSnap != null

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
            val pos = MouseInfo.getPointerInfo()?.location ?: return
            dragging = true
            moveOffsetX = pos.x - frame.x
            moveOffsetY = pos.y - frame.y
        }

        fun performDrag() {
            if (!dragging) return
            val pos = MouseInfo.getPointerInfo()?.location ?: return

            if (isSnapped) {
                val ratio = (pos.x - frame.x).toDouble() / frame.width.coerceAtLeast(1)
                unsnap()
                moveOffsetX = (frame.width * ratio).toInt()
                moveOffsetY = (pos.y - frame.y).coerceIn(0, frame.height)
            }

            frame.setLocation(pos.x - moveOffsetX, pos.y - moveOffsetY)
        }

        fun endDrag() {
            if (!dragging) return
            val pos = MouseInfo.getPointerInfo()?.location
            dragging = false
            if (pos != null) applySnap(pos.x, pos.y)
        }

        fun installDragArea(component: Component) {
            component.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent) {
                    val point = SwingUtilities.convertPoint(component, event.point, frame.glassPane)
                    if (SwingUtilities.isLeftMouseButton(event) && detectZone(point) == ResizeZone.NONE) {
                        dragging = true
                        moveOffsetX = event.xOnScreen - frame.x
                        moveOffsetY = event.yOnScreen - frame.y
                    }
                }

                override fun mouseReleased(event: MouseEvent) {
                    if (dragging) {
                        applySnap(event.xOnScreen, event.yOnScreen)
                        dragging = false
                    }
                }
            })

            component.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(event: MouseEvent) {
                    if (!dragging) return

                    if (isSnapped) {
                        val ratio = event.x.toDouble() / frame.width.coerceAtLeast(1)
                        unsnap()
                        moveOffsetX = (frame.width * ratio).toInt()
                        moveOffsetY = event.y
                    }

                    frame.setLocation(event.xOnScreen - moveOffsetX, event.yOnScreen - moveOffsetY)
                }
            })
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
            frame.bounds = saved
            boundsBeforeSnap = null
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
            var bestBounds = visualBounds(
                GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
            )
            var bestArea = 0L

            for (device in GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices) {
                val bounds = visualBounds(device.defaultConfiguration)
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
                val bounds = visualBounds(device.defaultConfiguration)
                if (x >= bounds.x && x < bounds.x + bounds.width && y >= bounds.y && y < bounds.y + bounds.height) {
                    return bounds
                }
            }

            return visualBounds(
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

    companion object {
        fun visualBounds(config: GraphicsConfiguration): Rectangle {
            val bounds = config.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
            return Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom
            )
        }
    }
}
