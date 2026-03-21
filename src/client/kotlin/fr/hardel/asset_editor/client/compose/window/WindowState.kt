package fr.hardel.asset_editor.client.compose.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class WindowPhase { SPLASH, EDITOR }

class WindowState {

    var phase: WindowPhase by mutableStateOf(WindowPhase.SPLASH)
        private set

    var splashMinTimeElapsed: Boolean by mutableStateOf(false)
        private set

    var permissionsReceived: Boolean by mutableStateOf(false)
        private set

    var resourceReloadTick: Int by mutableStateOf(0)
        private set

    fun markSplashTimerDone() {
        splashMinTimeElapsed = true
        tryTransition()
    }

    fun markPermissionsReceived() {
        permissionsReceived = true
        tryTransition()
    }

    fun onCreated() {
        phase = WindowPhase.SPLASH
        splashMinTimeElapsed = false
    }

    fun onWindowFocused() {}

    fun onWorldClosed() {
        phase = WindowPhase.SPLASH
        splashMinTimeElapsed = false
        permissionsReceived = false
    }

    fun onResourceReload() {
        if (phase == WindowPhase.EDITOR)
            resourceReloadTick++
    }

    private fun tryTransition() {
        if (splashMinTimeElapsed && permissionsReceived && phase == WindowPhase.SPLASH)
            phase = WindowPhase.EDITOR
    }
}
