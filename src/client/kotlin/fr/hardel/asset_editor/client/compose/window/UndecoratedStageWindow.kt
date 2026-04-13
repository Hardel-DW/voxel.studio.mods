package fr.hardel.asset_editor.client.compose.window

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Frame
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.AbstractAction
import javax.swing.JFrame
import javax.swing.KeyStroke
import javax.swing.WindowConstants
import org.slf4j.LoggerFactory

open class StageWindowHost(
    private val minWidth: Int,
    private val minHeight: Int
) {

    private val logger = LoggerFactory.getLogger(StageWindowHost::class.java)

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
            defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            minimumSize = Dimension(minWidth, minHeight)
            setSize(width, height)
            setLocation(
                screenBounds.x + (screenBounds.width - width) / 2,
                screenBounds.y + (screenBounds.height - height) / 2
            )
            background = Color.BLACK
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
            override fun windowActivated(event: WindowEvent) {
                onWindowFocused()
            }

            override fun windowClosing(event: WindowEvent) {
                hideWindow()
            }
        })
        createdFrame.addComponentListener(object : ComponentAdapter() {
            override fun componentMoved(event: ComponentEvent) {
                onFrameMetricsChanged()
            }

            override fun componentResized(event: ComponentEvent) {
                onFrameMetricsChanged()
            }
        })

        frame = createdFrame
        return true
    }

    protected open fun onWindowFocused() {}

    protected open fun onFrameMetricsChanged() {}

    protected fun setRoot(root: Component) {
        val currentFrame = frame ?: return
        currentFrame.contentPane.removeAll()
        currentFrame.contentPane.add(root)
        currentFrame.contentPane.revalidate()
        currentFrame.contentPane.repaint()
    }

    protected fun showWindow() {
        val currentFrame = frame ?: return
        currentFrame.extendedState = currentFrame.extendedState and Frame.ICONIFIED.inv()
        if (!currentFrame.isVisible) currentFrame.isVisible = true
        currentFrame.toFront()
        currentFrame.requestFocus()
    }

    protected fun hideWindow() {
        frame?.isVisible = false
    }

    protected fun toggleMaximize() {
        val currentFrame = frame ?: return
        val state = currentFrame.extendedState
        currentFrame.extendedState = if (state and Frame.MAXIMIZED_BOTH != 0) {
            state and Frame.MAXIMIZED_BOTH.inv()
        } else {
            state or Frame.MAXIMIZED_BOTH
        }
    }

    protected fun minimizeWindow() {
        val currentFrame = frame ?: return
        currentFrame.extendedState = currentFrame.extendedState or Frame.ICONIFIED
    }

    protected fun closeWindow() {
        hideWindow()
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
