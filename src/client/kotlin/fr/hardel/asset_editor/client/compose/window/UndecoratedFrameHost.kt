package fr.hardel.asset_editor.client.compose.window

import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame
import javax.swing.SwingUtilities

class UndecoratedFrameHost(
    private val frame: JFrame,
    private val resizeMargin: Int = 10,
    private val snapMargin: Int = 5
) {

    private var boundsBeforeSnap: Rectangle? = null
    private var resizing = false
    private var dragging = false
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
            override fun mousePressed(e: MouseEvent) {
                resizing = false
                dragging = false
                val zone = detectZone(e.point)
                if (zone != ResizeZone.NONE && !isSnapped) {
                    activeZone = zone
                    resizing = true
                    dragOriginX = e.xOnScreen
                    dragOriginY = e.yOnScreen
                    dragFrameX = frame.x
                    dragFrameY = frame.y
                    dragFrameW = frame.width
                    dragFrameH = frame.height
                    return
                }
                activeZone = ResizeZone.NONE
            }

            override fun mouseReleased(e: MouseEvent) {
                if (resizing) {
                    resizing = false
                    glass.cursor = Cursor.getDefaultCursor()
                    return
                }
                if (dragging) {
                    applySnap(e.xOnScreen, e.yOnScreen)
                    dragging = false
                }
            }

            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2 && detectZone(e.point) == ResizeZone.NONE) {
                    toggleMaximize()
                }
                redispatchToContent(e)
            }
        })

        glass.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                if (!resizing && !dragging) {
                    val zone = if (isSnapped) ResizeZone.NONE else detectZone(e.point)
                    glass.cursor = zone.cursor
                    if (zone == ResizeZone.NONE) redispatchToContent(e)
                }
            }

            override fun mouseDragged(e: MouseEvent) {
                if (resizing) {
                    val dx = e.xOnScreen - dragOriginX
                    val dy = e.yOnScreen - dragOriginY
                    activeZone.apply(frame, dx, dy, dragFrameW, dragFrameH, dragFrameX, dragFrameY)
                    return
                }
                redispatchToContent(e)
            }
        })
    }

    fun installDragArea(component: java.awt.Component) {
        component.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e) && detectZone(SwingUtilities.convertPoint(component, e.point, frame.glassPane)) == ResizeZone.NONE) {
                    dragging = true
                    moveOffsetX = e.xOnScreen - frame.x
                    moveOffsetY = e.yOnScreen - frame.y
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (dragging) {
                    applySnap(e.xOnScreen, e.yOnScreen)
                    dragging = false
                }
            }
        })

        component.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (!dragging) return
                if (isSnapped) {
                    val ratio = e.x.toDouble() / frame.width.coerceAtLeast(1)
                    unsnap()
                    moveOffsetX = (frame.width * ratio).toInt()
                    moveOffsetY = e.y
                }
                frame.setLocation(e.xOnScreen - moveOffsetX, e.yOnScreen - moveOffsetY)
            }
        })
    }

    fun toggleMaximize() {
        if (isSnapped) unsnap() else snapTo(screenBoundsAt(frame.x + frame.width / 2, frame.y + frame.height / 2))
    }

    fun snapTo(region: Rectangle) {
        if (boundsBeforeSnap == null) boundsBeforeSnap = frame.bounds
        frame.bounds = region
    }

    fun unsnap() {
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

        val hw = bounds.width / 2
        val hh = bounds.height / 2
        val bx = bounds.x
        val by = bounds.y

        when {
            top && !left && !right -> snapTo(bounds)
            left && top -> snapTo(Rectangle(bx, by, hw, hh))
            right && top -> snapTo(Rectangle(bx + hw, by, hw, hh))
            left && bottom -> snapTo(Rectangle(bx, by + hh, hw, hh))
            right && bottom -> snapTo(Rectangle(bx + hw, by + hh, hw, hh))
            left -> snapTo(Rectangle(bx, by, hw, bounds.height))
            right -> snapTo(Rectangle(bx + hw, by, hw, bounds.height))
            bottom -> snapTo(Rectangle(bx, by + hh, bounds.width, hh))
        }
    }

    private fun screenBoundsAt(x: Int, y: Int): Rectangle {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        for (device in ge.screenDevices) {
            val b = device.defaultConfiguration.bounds
            if (x >= b.x && x < b.x + b.width && y >= b.y && y < b.y + b.height) return b
        }
        return ge.defaultScreenDevice.defaultConfiguration.bounds
    }

    private fun detectZone(p: Point): ResizeZone {
        val w = frame.width
        val h = frame.height
        val n = p.y <= resizeMargin
        val s = p.y >= h - resizeMargin
        val west = p.x <= resizeMargin
        val east = p.x >= w - resizeMargin

        return when {
            n && west -> ResizeZone.NW
            n && east -> ResizeZone.NE
            s && west -> ResizeZone.SW
            s && east -> ResizeZone.SE
            n -> ResizeZone.N
            s -> ResizeZone.S
            west -> ResizeZone.W
            east -> ResizeZone.E
            else -> ResizeZone.NONE
        }
    }

    private fun redispatchToContent(e: MouseEvent) {
        val glass = frame.glassPane
        val contentPane = frame.contentPane
        val p = SwingUtilities.convertPoint(glass, e.point, contentPane)
        val target = SwingUtilities.getDeepestComponentAt(contentPane, p.x, p.y) ?: return
        val targetPoint = SwingUtilities.convertPoint(contentPane, p, target)
        target.dispatchEvent(MouseEvent(
            target, e.id, e.`when`, e.modifiersEx,
            targetPoint.x, targetPoint.y, e.xOnScreen, e.yOnScreen,
            e.clickCount, e.isPopupTrigger, e.button
        ))
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

        fun apply(f: JFrame, dx: Int, dy: Int, ow: Int, oh: Int, ox: Int, oy: Int) {
            val minW = f.minimumSize.width
            val minH = f.minimumSize.height
            when (this) {
                N -> { val h = (oh - dy).coerceAtLeast(minH); f.setBounds(ox, oy + oh - h, ow, h) }
                S -> f.setSize(ow, (oh + dy).coerceAtLeast(minH))
                W -> { val w = (ow - dx).coerceAtLeast(minW); f.setBounds(ox + ow - w, oy, w, oh) }
                E -> f.setSize((ow + dx).coerceAtLeast(minW), oh)
                NW -> { N.apply(f, dx, dy, ow, oh, ox, oy); W.apply(f, dx, dy, ow, oh, ox, oy) }
                NE -> { N.apply(f, dx, dy, ow, oh, ox, oy); E.apply(f, dx, dy, ow, oh, ox, oy) }
                SW -> { S.apply(f, dx, dy, ow, oh, ox, oy); W.apply(f, dx, dy, ow, oh, ox, oy) }
                SE -> { S.apply(f, dx, dy, ow, oh, ox, oy); E.apply(f, dx, dy, ow, oh, ox, oy) }
                NONE -> {}
            }
        }
    }
}
