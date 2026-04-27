package fr.hardel.asset_editor.client.compose.window

import fr.hardel.asset_editor.client.StudioActivityTracker
import fr.hardel.asset_editor.client.splash.SplashAssets
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Frame
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
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
import javax.swing.SwingUtilities
import org.slf4j.LoggerFactory

/**
 * Decorated AWT/Swing stage window driven by Minecraft lifecycle events.
 * The OS owns the title bar, drag, resize and snap; we only manage the content area.
 */
abstract class StageWindow(
    private val minWidth: Int,
    private val minHeight: Int,
    private val titleBarColor: Color = Color.BLACK
) {

    private val logger = LoggerFactory.getLogger(StageWindow::class.java)

    protected var frame: JFrame? = null
        private set

    private var startupRequested = false

    @Volatile
    private var platformReady = false

    @Volatile
    private var pendingWorldClose = false

    protected abstract fun onCreated()
    protected open fun onBeforeShow() {}
    protected open fun onWindowFocused() {}
    protected open fun onWorldClosed() {}
    protected open fun onResourceReload() {}

    fun isPlatformReady(): Boolean = platformReady

    fun open() {
        SwingUtilities.invokeLater {
            if (startupRequested) {
                onWindowFocused()
                showWindow()
                return@invokeLater
            }
            startupRequested = true
            if (!createFrame()) return@invokeLater
            platformReady = true

            onBeforeShow()
            onWindowFocused()
            showWindow()

            SwingUtilities.invokeLater {
                onCreated()
                if (pendingWorldClose) {
                    pendingWorldClose = false
                    onWorldClosed()
                }
            }
        }
    }

    fun fireWorldClosed() {
        if (platformReady) SwingUtilities.invokeLater(::onWorldClosed)
        else if (startupRequested) pendingWorldClose = true
    }

    fun fireResourceReload() {
        if (platformReady) SwingUtilities.invokeLater(::onResourceReload)
    }

    protected fun setRoot(root: Component) {
        val current = frame ?: return
        current.contentPane.removeAll()
        current.contentPane.add(root)
        current.contentPane.revalidate()
        current.contentPane.repaint()
    }

    protected fun showWindow() {
        val current = frame ?: return
        if (!current.isVisible) current.isVisible = true
        StudioActivityTracker.markVisible()
        current.toFront()
        current.requestFocus()
        WindowsTitleBar.apply(current, titleBarColor)
    }

    protected fun hideWindow() {
        StudioActivityTracker.markHidden()
        frame?.isVisible = false
    }

    private fun toggleFullscreen() {
        val current = frame ?: return
        current.extendedState = if (current.extendedState and Frame.MAXIMIZED_BOTH != 0) {
            Frame.NORMAL
        } else {
            Frame.MAXIMIZED_BOTH
        }
    }

    private fun JComponent.bindShortcut(keyCode: Int, name: String, action: () -> Unit) {
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), name)
        actionMap.put(name, object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent?) = action()
        })
    }

    private fun createFrame(): Boolean {
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
            defaultCloseOperation = JFrame.HIDE_ON_CLOSE
        }

        (createdFrame.contentPane as? JPanel)?.apply {
            background = Color.BLACK
            isOpaque = true
        }

        createdFrame.rootPane.apply {
            bindShortcut(KeyEvent.VK_ESCAPE, "window-hide-on-escape", ::hideWindow)
            bindShortcut(KeyEvent.VK_F11, "window-toggle-fullscreen", ::toggleFullscreen)
        }

        createdFrame.addWindowListener(object : WindowAdapter() {
            override fun windowOpened(event: WindowEvent) = StudioActivityTracker.markVisible()

            override fun windowClosing(event: WindowEvent) = StudioActivityTracker.markHidden()

            override fun windowClosed(event: WindowEvent) {
                StudioActivityTracker.markHidden()
                if (frame === createdFrame) frame = null
            }

            override fun windowActivated(event: WindowEvent) {
                StudioActivityTracker.markFocused()
                onWindowFocused()
            }

            override fun windowDeactivated(event: WindowEvent) = StudioActivityTracker.markUnfocused()

            override fun windowIconified(event: WindowEvent) = StudioActivityTracker.markHidden()

            override fun windowDeiconified(event: WindowEvent) = StudioActivityTracker.markVisible()
        })

        frame = createdFrame
        return true
    }

    companion object {
        @Volatile
        private var runtimePrepared = false

        @JvmStatic
        fun initializeRuntime() {
            if (runtimePrepared) return
            synchronized(this) {
                if (runtimePrepared) return
                System.setProperty("java.awt.headless", "false")
                runtimePrepared = true
            }
        }

        private fun visualBounds(config: GraphicsConfiguration): Rectangle {
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
