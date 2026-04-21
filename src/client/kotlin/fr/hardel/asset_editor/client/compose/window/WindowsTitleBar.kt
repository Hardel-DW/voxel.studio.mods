package fr.hardel.asset_editor.client.compose.window

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import java.awt.Color
import java.awt.Window
import org.slf4j.LoggerFactory

/**
 * Tints the Windows OS title bar via DWM attributes:
 * - DWMWA_USE_IMMERSIVE_DARK_MODE forces white-on-dark text/buttons regardless of system theme.
 * - DWMWA_CAPTION_COLOR / DWMWA_BORDER_COLOR paint the caption + 1px border in the studio palette.
 *
 * No-op on non-Windows or when dwmapi.dll fails to load (Linux, macOS, very old Windows).
 */
internal object WindowsTitleBar {

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_BORDER_COLOR = 34
    private const val DWMWA_CAPTION_COLOR = 35

    private val logger = LoggerFactory.getLogger(WindowsTitleBar::class.java)

    private val library: Dwmapi? by lazy {
        try {
            Native.load("dwmapi", Dwmapi::class.java)
        } catch (t: Throwable) {
            logger.debug("dwmapi unavailable: {}", t.message)
            null
        }
    }

    fun apply(window: Window, captionColor: Color) {
        setAttribute(window, DWMWA_USE_IMMERSIVE_DARK_MODE, 1)
        val colorRef = (captionColor.blue shl 16) or (captionColor.green shl 8) or captionColor.red
        setAttribute(window, DWMWA_CAPTION_COLOR, colorRef)
        setAttribute(window, DWMWA_BORDER_COLOR, colorRef)
    }

    private fun setAttribute(window: Window, attribute: Int, value: Int) {
        if (!window.isDisplayable) return
        val lib = library ?: return
        val pointer = Native.getComponentPointer(window) ?: return
        val ref = IntByReference(value)
        try {
            lib.DwmSetWindowAttribute(HWND(pointer), attribute, ref, 4)
        } catch (t: Throwable) {
            logger.debug("DwmSetWindowAttribute({}) failed: {}", attribute, t.message)
        }
    }

    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, pvAttribute: IntByReference, cbAttribute: Int): Int
    }
}
