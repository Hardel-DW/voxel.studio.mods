package fr.hardel.asset_editor.client.compose.window

import com.formdev.flatlaf.FlatDarkLaf
import java.awt.Color
import javax.swing.SwingUtilities
import javax.swing.UIManager

abstract class MinecraftStageWindow(
    minWidth: Int,
    minHeight: Int
) : UndecoratedStageWindow(minWidth, minHeight) {

    private var startupRequested = false

    @Volatile
    private var platformReady = false

    @Volatile
    private var pendingWorldClose = false

    protected abstract fun onCreated()

    protected open fun onWorldClosed() {}

    protected open fun onResourceReload() {}

    fun isPlatformReady(): Boolean = platformReady

    fun open() {
        SwingUtilities.invokeLater {
            if (!startupRequested) {
                startupRequested = true
                if (!initializeWindow()) return@invokeLater
                platformReady = true
                onCreated()
                if (pendingWorldClose) {
                    pendingWorldClose = false
                    onWorldClosed()
                    return@invokeLater
                }
            }

            onWindowFocused()
            showWindow()
        }
    }

    fun fireWorldClosed() {
        if (platformReady)
            SwingUtilities.invokeLater(this::onWorldClosed)
        else if (startupRequested)
            pendingWorldClose = true
    }

    fun fireResourceReload() {
        if (platformReady)
            SwingUtilities.invokeLater(this::onResourceReload)
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
                System.setProperty("sun.awt.noerasebackground", "true")
                FlatDarkLaf.setup()
                UIManager.put("Panel.background", Color.BLACK)
                UIManager.put("RootPane.background", Color.BLACK)
                runtimePrepared = true
            }
        }
    }
}
