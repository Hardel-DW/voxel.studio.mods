package fr.hardel.asset_editor.client.compose.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import com.formdev.flatlaf.FlatClientProperties
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WindowProc
import com.sun.jna.win32.W32APIOptions
import java.awt.Color
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.AbstractAction
import javax.swing.JFrame
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import org.slf4j.LoggerFactory

open class UndecoratedStageWindow(
    private val minWidth: Int,
    private val minHeight: Int
) {

    private val logger = LoggerFactory.getLogger(UndecoratedStageWindow::class.java)

    protected var composeWindow: ComposeWindow? = null
        private set

    private val dragRegions = ConcurrentHashMap<String, Rectangle>()
    private var nativeDragSupport: NativeWindowDragSupport? = null

    protected fun initializeWindow(): Boolean {
        if (composeWindow != null) return true

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

        val createdWindow = ComposeWindow().apply {
            minimumSize = Dimension(minWidth, minHeight)
            setSize(width, height)
            setLocation(
                screenBounds.x + (screenBounds.width - width) / 2,
                screenBounds.y + (screenBounds.height - height) / 2
            )
            background = Color.BLACK
            contentPane.background = Color.BLACK
            rootPane.background = Color.BLACK
            layeredPane.background = Color.BLACK
            glassPane.background = Color.BLACK
        }

        applyWindowDecorationProperties(createdWindow)
        installEscapeToHide(createdWindow)
        installWindowListeners(createdWindow)

        composeWindow = createdWindow
        return true
    }

    private fun applyWindowDecorationProperties(window: ComposeWindow) {
        val rootPane = window.rootPane

        rootPane.putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, true)
        rootPane.putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true)
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false)
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICON, false)
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICONIFFY, false)
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_MAXIMIZE, false)
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_CLOSE, false)
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_HEIGHT, TITLE_BAR_HEIGHT_DP)

        rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
    }

    private fun installEscapeToHide(window: ComposeWindow) {
        window.rootPane.apply {
            val action = "window-hide-on-escape"
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), action)
            actionMap.put(action, object : AbstractAction() {
                override fun actionPerformed(event: ActionEvent?) {
                    hideWindow()
                }
            })
        }
    }

    private fun installWindowListeners(window: ComposeWindow) {
        window.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                if (composeWindow === window) {
                    nativeDragSupport = null
                    composeWindow = null
                    dragRegions.clear()
                }
            }

            override fun windowActivated(event: WindowEvent) {
                onWindowFocused()
            }
        })
    }

    protected open fun onWindowFocused() {}

    protected fun setComposeContent(content: @Composable () -> Unit) {
        val window = composeWindow ?: return
        window.setContent { content() }
    }

    fun registerDragRegion(id: String, bounds: Rectangle) {
        dragRegions[id] = bounds
    }

    fun unregisterDragRegion(id: String) {
        dragRegions.remove(id)
    }

    protected fun showWindow() {
        val window = composeWindow ?: return
        if (!window.isVisible) window.isVisible = true
        nativeDragSupport?.ensureInstalled()
            ?: run {
                nativeDragSupport = NativeWindowDragSupport.create(window) { point ->
                    dragRegions.values.any { it.contains(point) }
                }?.also { it.ensureInstalled() }
            }
        window.toFront()
        window.requestFocus()
    }

    protected fun hideWindow() {
        composeWindow?.isVisible = false
    }

    protected fun toggleMaximize() {
        val window = composeWindow ?: return
        val isMaximized = (window.extendedState and JFrame.MAXIMIZED_BOTH) != 0
        window.extendedState = if (isMaximized) {
            JFrame.NORMAL
        } else {
            JFrame.MAXIMIZED_BOTH
        }
    }

    protected fun minimizeWindow() {
        composeWindow?.extendedState = JFrame.ICONIFIED
    }

    protected fun closeWindow() {
        composeWindow?.isVisible = false
    }

    companion object {
        private const val TITLE_BAR_HEIGHT_DP = 48

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

    private class NativeWindowDragSupport private constructor(
        private val window: ComposeWindow,
        private val isDragRegionHit: (Point) -> Boolean,
        private val user32: User32Ex
    ) : WindowProc {

        private var installed = false
        private var previousWndProc = LONG_PTR(0)

        fun ensureInstalled() {
            if (installed || !window.isDisplayable) return

            val handle = window.windowHandle
            if (handle == 0L) return

            val hwnd = HWND(Pointer(handle))
            previousWndProc = if (Native.POINTER_SIZE == 8) {
                user32.SetWindowLongPtr(hwnd, GWLP_WNDPROC, this)
            } else {
                LONG_PTR(user32.SetWindowLong(hwnd, GWLP_WNDPROC, this).toLong())
            }
            installed = true
        }

        override fun callback(hWnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
            return when (uMsg) {
                WM_NCHITTEST -> handleNcHitTest(hWnd, uMsg, wParam, lParam)
                else -> callDefault(hWnd, uMsg, wParam, lParam)
            }
        }

        private fun handleNcHitTest(
            hWnd: HWND,
            uMsg: Int,
            wParam: WPARAM,
            lParam: LPARAM
        ): LRESULT {
            val defaultResult = callDefault(hWnd, uMsg, wParam, lParam)
            if (defaultResult.toInt() != HTCLIENT) return defaultResult

            val point = Point(xFromLParam(lParam), yFromLParam(lParam))
            val pointInContent = screenToContent(point) ?: return defaultResult

            return if (isDragRegionHit(pointInContent)) {
                LRESULT(HTCAPTION.toLong())
            } else {
                defaultResult
            }
        }

        private fun callDefault(hWnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
            return user32.CallWindowProc(previousWndProc, hWnd, uMsg, wParam, lParam)
        }

        private fun screenToContent(screenPoint: Point): Point? {
            if (!window.isShowing) return null
            return Point(screenPoint).apply {
                SwingUtilities.convertPointFromScreen(this, window.contentPane)
            }
        }

        companion object {
            private const val GWLP_WNDPROC = -4
            private const val WM_NCHITTEST = 0x0084
            private const val HTCLIENT = 1
            private const val HTCAPTION = 2

            fun create(
                window: ComposeWindow,
                isDragRegionHit: (Point) -> Boolean
            ): NativeWindowDragSupport? {
                if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                    return null
                }

                val user32 = runCatching {
                    Native.load("user32", User32Ex::class.java, W32APIOptions.DEFAULT_OPTIONS)
                }.getOrNull() ?: return null

                return NativeWindowDragSupport(window, isDragRegionHit, user32)
            }

            private fun xFromLParam(lParam: LPARAM): Int {
                return (lParam.toInt() and 0xFFFF).toShort().toInt()
            }

            private fun yFromLParam(lParam: LPARAM): Int {
                return ((lParam.toInt() ushr 16) and 0xFFFF).toShort().toInt()
            }
        }
    }

    @Suppress("FunctionName")
    private interface User32Ex : User32 {
        fun SetWindowLong(hWnd: HWND, nIndex: Int, wndProc: WindowProc): Int
        fun SetWindowLongPtr(hWnd: HWND, nIndex: Int, wndProc: WindowProc): LONG_PTR
        fun CallWindowProc(
            previousWindowProc: LONG_PTR,
            hWnd: HWND,
            uMsg: Int,
            wParam: WPARAM,
            lParam: LPARAM
        ): LRESULT
    }
}
