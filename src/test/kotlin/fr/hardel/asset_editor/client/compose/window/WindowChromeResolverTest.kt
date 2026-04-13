@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package fr.hardel.asset_editor.client.compose.window

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WindowChromeResolverTest {

    @Test
    fun `uses jbr custom title bar on windows when jbr decorations are supported`() {
        val mode = WindowChromeResolver.resolve(
            WindowChromeEnvironment(
                platform = WindowPlatform.WINDOWS,
                backend = WindowingBackend.UNKNOWN,
                jbrAvailable = true,
                windowDecorationsSupported = true
            )
        )

        assertEquals(WindowChromeMode.JBR_CUSTOM_TITLEBAR, mode)
    }

    @Test
    fun `uses jbr custom title bar on macos when jbr decorations are supported`() {
        val mode = WindowChromeResolver.resolve(
            WindowChromeEnvironment(
                platform = WindowPlatform.MACOS,
                backend = WindowingBackend.UNKNOWN,
                jbrAvailable = true,
                windowDecorationsSupported = true
            )
        )

        assertEquals(WindowChromeMode.JBR_CUSTOM_TITLEBAR, mode)
    }

    @Test
    fun `linux stays on native system chrome on x11 and wayland`() {
        val x11Mode = WindowChromeResolver.resolve(
            WindowChromeEnvironment(
                platform = WindowPlatform.LINUX,
                backend = WindowingBackend.X11,
                jbrAvailable = true,
                windowDecorationsSupported = true
            )
        )
        val waylandMode = WindowChromeResolver.resolve(
            WindowChromeEnvironment(
                platform = WindowPlatform.LINUX,
                backend = WindowingBackend.WAYLAND,
                jbrAvailable = true,
                windowDecorationsSupported = true
            )
        )

        assertEquals(WindowChromeMode.NATIVE_SYSTEM, x11Mode)
        assertEquals(WindowChromeMode.NATIVE_SYSTEM, waylandMode)
    }

    @Test
    fun `detects windowing backend from toolkit class names`() {
        assertEquals(WindowingBackend.X11, WindowChromeResolver.detectBackend("sun.awt.X11.XToolkit"))
        assertEquals(WindowingBackend.WAYLAND, WindowChromeResolver.detectBackend("sun.awt.wl.WLToolkit"))
        assertEquals(WindowingBackend.UNKNOWN, WindowChromeResolver.detectBackend("sun.awt.windows.WToolkit"))
    }

    @Test
    fun `detects operating systems from os name`() {
        assertEquals(WindowPlatform.WINDOWS, WindowChromeResolver.detectPlatform("Windows 11"))
        assertEquals(WindowPlatform.MACOS, WindowChromeResolver.detectPlatform("Mac OS X"))
        assertEquals(WindowPlatform.LINUX, WindowChromeResolver.detectPlatform("Linux"))
        assertEquals(WindowPlatform.OTHER, WindowChromeResolver.detectPlatform("Solaris"))
    }
}
