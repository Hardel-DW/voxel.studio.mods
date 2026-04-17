package fr.hardel.asset_editor.client.compose.window.chrome

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import java.awt.Window
import org.slf4j.LoggerFactory

/**
 * Thin JNA wrapper around the handful of DWM attributes we use for Windows 11 polish.
 * All calls are guarded — if the library can't load (non-Windows or old OS), methods no-op.
 */
internal object DwmApi {

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33

    const val CORNER_DEFAULT = 0
    const val CORNER_DO_NOT_ROUND = 1
    const val CORNER_ROUND = 2
    const val CORNER_ROUND_SMALL = 3

    private val logger = LoggerFactory.getLogger(DwmApi::class.java)

    private val library: Dwmapi? by lazy {
        try {
            Native.load("dwmapi", Dwmapi::class.java)
        } catch (t: Throwable) {
            logger.debug("dwmapi unavailable: {}", t.message)
            null
        }
    }

    fun setDarkMode(window: Window, enabled: Boolean) {
        setAttribute(window, DWMWA_USE_IMMERSIVE_DARK_MODE, if (enabled) 1 else 0)
    }

    fun setCornerPreference(window: Window, preference: Int) {
        setAttribute(window, DWMWA_WINDOW_CORNER_PREFERENCE, preference)
    }

    private fun setAttribute(window: Window, attribute: Int, value: Int) {
        val lib = library ?: return
        val handle = hwnd(window) ?: return
        val ref = IntByReference(value)
        try {
            lib.DwmSetWindowAttribute(handle, attribute, ref, 4)
        } catch (t: Throwable) {
            logger.debug("DwmSetWindowAttribute({}) failed: {}", attribute, t.message)
        }
    }

    private fun hwnd(window: Window): HWND? {
        if (!window.isDisplayable) return null
        val pointer = Native.getComponentPointer(window) ?: return null
        return HWND(pointer)
    }

    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, pvAttribute: IntByReference, cbAttribute: Int): Int
    }
}
