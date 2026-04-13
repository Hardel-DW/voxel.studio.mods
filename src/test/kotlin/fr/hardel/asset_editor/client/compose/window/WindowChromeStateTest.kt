@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package fr.hardel.asset_editor.client.compose.window

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WindowChromeStateTest {

    @Test
    fun `native system chrome exposes no overlay metrics`() {
        val state = WindowChromeState.nativeSystem()

        assertEquals(WindowChromeMode.NATIVE_SYSTEM, state.mode)
        assertEquals(0, state.titleBarHeightPx)
        assertEquals(0, state.leftInsetPx)
        assertEquals(0, state.rightInsetPx)
        assertTrue(state.nativeControlsVisible)
    }

    @Test
    fun `custom title bar metrics are clamped to valid values`() {
        val state = WindowChromeState.customTitleBar(
            titleBarHeightPx = 48,
            leftInsetPx = -12,
            rightInsetPx = 64
        )

        assertEquals(WindowChromeMode.JBR_CUSTOM_TITLEBAR, state.mode)
        assertEquals(48, state.titleBarHeightPx)
        assertEquals(0, state.leftInsetPx)
        assertEquals(64, state.rightInsetPx)
        assertTrue(state.nativeControlsVisible)
    }

    @Test
    fun `title bar height defaults stay aligned with compose layouts`() {
        assertEquals(32, WindowChromeDefaults.SPLASH_TITLE_BAR_HEIGHT_DP)
        assertEquals(48, WindowChromeDefaults.EDITOR_TITLE_BAR_HEIGHT_DP)
    }
}
