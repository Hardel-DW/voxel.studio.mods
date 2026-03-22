package fr.hardel.asset_editor.client.compose.window

import androidx.compose.ui.awt.ComposePanel
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import javax.swing.JFrame
import javax.swing.SwingUtilities

object ComposeStudioWindow {

    private val logger = LoggerFactory.getLogger(ComposeStudioWindow::class.java)
    private const val MIN_WIDTH = 680
    private const val MIN_HEIGHT = 440

    @Volatile
    private var frame: JFrame? = null

    @Volatile
    private var frameHost: UndecoratedFrameHost? = null

    @Volatile
    private var runtimePrepared = false

    private val windowState = WindowState()

    @JvmStatic
    fun initializeRuntime() {
        if (runtimePrepared) return
        synchronized(this) {
            if (runtimePrepared) return
            System.setProperty("java.awt.headless", "false")
            runtimePrepared = true
        }
    }

    @JvmStatic
    fun requestOpen() {
        VoxelResourceLoader.update(Minecraft.getInstance().resourceManager)
        SwingUtilities.invokeLater(::openOrFocusWindow)
    }

    @JvmStatic
    fun notifyWorldClosed() {
        SwingUtilities.invokeLater {
            windowState.onWorldClosed()
            disposeWindow()
        }
    }

    @JvmStatic
    fun notifyResourceReload() {
        SwingUtilities.invokeLater { windowState.onResourceReload() }
    }

    @JvmStatic
    fun requestToggleMaximize() {
        SwingUtilities.invokeLater { frameHost?.toggleMaximize() }
    }

    @JvmStatic
    fun requestMinimize() {
        SwingUtilities.invokeLater { frame?.state = JFrame.ICONIFIED }
    }

    @JvmStatic
    fun requestClose() {
        SwingUtilities.invokeLater { frame?.isVisible = false }
    }

    private fun openOrFocusWindow() {
        if (GraphicsEnvironment.isHeadless()) {
            logger.error("Cannot open Compose window: AWT is headless. java.awt.headless={}", System.getProperty("java.awt.headless"))
            return
        }

        val existing = frame
        if (existing != null) {
            if (!existing.isVisible) existing.isVisible = true
            existing.toFront()
            existing.requestFocus()
            windowState.onWindowFocused()
            return
        }

        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenBounds = ge.defaultScreenDevice.defaultConfiguration.bounds
        val width = (screenBounds.width * 0.75).toInt().coerceAtLeast(MIN_WIDTH)
        val height = (screenBounds.height * 0.75).toInt().coerceAtLeast(MIN_HEIGHT)

        val createdFrame = JFrame()
        createdFrame.isUndecorated = true
        createdFrame.title = "Voxel Studio"
        createdFrame.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
        createdFrame.setSize(width, height)
        createdFrame.setLocationRelativeTo(null)
        createdFrame.background = java.awt.Color.BLACK

        val host = UndecoratedFrameHost(createdFrame)
        host.install()

        val composePanel = ComposePanel()
        composePanel.setContent {
            ComposeStudioContent(windowState)
        }
        createdFrame.contentPane.add(composePanel)

        createdFrame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                if (frame === createdFrame) {
                    frame = null
                    frameHost = null
                }
            }

            override fun windowActivated(e: WindowEvent) {
                windowState.onWindowFocused()
            }
        })

        frame = createdFrame
        frameHost = host
        createdFrame.isVisible = true
        windowState.onCreated()
    }

    private fun disposeWindow() {
        val existing = frame ?: return
        frame = null
        frameHost = null
        existing.dispose()
    }
}
