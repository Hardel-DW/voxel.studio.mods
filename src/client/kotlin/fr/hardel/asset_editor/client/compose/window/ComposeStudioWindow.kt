package fr.hardel.asset_editor.client.compose.window

import androidx.compose.ui.awt.ComposePanel
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import org.slf4j.LoggerFactory
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

object ComposeStudioWindow {

    private val logger = LoggerFactory.getLogger(ComposeStudioWindow::class.java)

    @Volatile
    private var frame: JFrame? = null

    @Volatile
    private var runtimePrepared = false

    @JvmStatic
    fun initializeRuntime() {
        if (runtimePrepared)
            return

        synchronized(this) {
            if (runtimePrepared)
                return

            System.setProperty("java.awt.headless", "false")
            runtimePrepared = true
        }
    }

    @JvmStatic
    fun requestOpen() {
        SwingUtilities.invokeLater(::openOrFocusWindow)
    }

    @JvmStatic
    fun notifyWorldClosed() {
        SwingUtilities.invokeLater(::disposeWindow)
    }

    @JvmStatic
    fun notifyResourceReload() {
    }

    private fun openOrFocusWindow() {
        if (GraphicsEnvironment.isHeadless()) {
            logger.error(
                "Cannot open Compose window because AWT is headless in this client runtime. java.awt.headless={}",
                System.getProperty("java.awt.headless"))
            return
        }

        val existing = frame
        if (existing != null) {
            if (!existing.isVisible)
                existing.isVisible = true
            existing.toFront()
            existing.requestFocus()
            return
        }

        val composePanel = ComposePanel()
        composePanel.setContent {
            ComposeStudioContent()
        }

        val createdFrame = JFrame("Voxel Studio Compose")
        createdFrame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        createdFrame.minimumSize = Dimension(680, 440)
        createdFrame.setSize(960, 640)
        createdFrame.setLocationRelativeTo(null)
        createdFrame.contentPane.add(composePanel)
        createdFrame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                if (frame === createdFrame)
                    frame = null
            }
        })

        frame = createdFrame
        createdFrame.isVisible = true
    }

    private fun disposeWindow() {
        val existing = frame ?: return
        frame = null
        existing.dispose()
    }
}
