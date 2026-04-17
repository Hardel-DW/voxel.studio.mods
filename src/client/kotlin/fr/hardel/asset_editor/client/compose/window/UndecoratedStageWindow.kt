package fr.hardel.asset_editor.client.compose.window

import androidx.compose.ui.awt.ComposePanel
import fr.hardel.asset_editor.client.compose.window.chrome.NativeChromeFactory
import fr.hardel.asset_editor.client.compose.window.chrome.NativeWindowChrome
import fr.hardel.asset_editor.client.splash.SplashAssets
import java.awt.Color
import java.awt.Component
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
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.KeyStroke
import org.slf4j.LoggerFactory

open class UndecoratedStageWindow(
    private val minWidth: Int,
    private val minHeight: Int
) {

    private val logger = LoggerFactory.getLogger(UndecoratedStageWindow::class.java)

    protected val chrome: NativeWindowChrome = NativeChromeFactory.create()

    protected var frame: JFrame? = null
        private set

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
            minimumSize = Dimension(minWidth, minHeight)
            setSize(width, height)
            setLocation(
                screenBounds.x + (screenBounds.width - width) / 2,
                screenBounds.y + (screenBounds.height - height) / 2
            )
            background = Color.BLACK
            iconImages = SplashAssets.logoIcons()
        }

        chrome.applyTo(createdFrame)

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

        createdFrame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                if (frame === createdFrame) {
                    chrome.dispose()
                    frame = null
                }
            }

            override fun windowActivated(event: WindowEvent) {
                onWindowFocused()
            }
        })

        frame = createdFrame
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

    protected fun attachSwingContent(panel: JComponent, captionHitTest: (Point) -> Boolean) {
        chrome.attachSwingContent(panel, captionHitTest)
    }

    protected fun attachComposeContent(panel: ComposePanel) {
        chrome.attachComposeContent(panel)
    }

    protected fun showWindow() {
        val currentFrame = frame ?: return
        if (!currentFrame.isVisible) currentFrame.isVisible = true
        currentFrame.toFront()
        currentFrame.requestFocus()
        chrome.onFrameShown(currentFrame)
    }

    protected fun hideWindow() {
        frame?.isVisible = false
    }

    protected fun toggleMaximize() {
        chrome.toggleMaximize()
    }

    protected fun minimizeWindow() {
        frame?.state = JFrame.ICONIFIED
    }

    protected fun closeWindow() {
        frame?.isVisible = false
    }

    protected fun beginFrameDrag() {
        chrome.beginDrag()
    }

    protected fun performFrameDrag() {
        chrome.performDrag()
    }

    protected fun endFrameDrag() {
        chrome.endDrag()
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
