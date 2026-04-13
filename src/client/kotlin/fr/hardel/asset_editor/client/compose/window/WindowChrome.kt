package fr.hardel.asset_editor.client.compose.window

import androidx.compose.runtime.Immutable
import com.jetbrains.JBR
import java.awt.Toolkit
import java.util.Locale

internal enum class WindowChromeMode {
    NATIVE_SYSTEM,
    JBR_CUSTOM_TITLEBAR
}

internal enum class WindowPlatform {
    WINDOWS,
    MACOS,
    LINUX,
    OTHER
}

internal enum class WindowingBackend {
    X11,
    WAYLAND,
    UNKNOWN
}

internal object WindowChromeDefaults {
    const val SPLASH_TITLE_BAR_HEIGHT_DP = 32
    const val EDITOR_TITLE_BAR_HEIGHT_DP = 48
}

@Immutable
internal data class WindowChromeState(
    val mode: WindowChromeMode,
    val titleBarHeightPx: Int,
    val leftInsetPx: Int,
    val rightInsetPx: Int,
    val nativeControlsVisible: Boolean
) {
    companion object {
        fun nativeSystem(): WindowChromeState {
            return WindowChromeState(
                mode = WindowChromeMode.NATIVE_SYSTEM,
                titleBarHeightPx = 0,
                leftInsetPx = 0,
                rightInsetPx = 0,
                nativeControlsVisible = true
            )
        }

        fun customTitleBar(
            titleBarHeightPx: Int,
            leftInsetPx: Int,
            rightInsetPx: Int,
            nativeControlsVisible: Boolean = true
        ): WindowChromeState {
            return WindowChromeState(
                mode = WindowChromeMode.JBR_CUSTOM_TITLEBAR,
                titleBarHeightPx = titleBarHeightPx.coerceAtLeast(1),
                leftInsetPx = leftInsetPx.coerceAtLeast(0),
                rightInsetPx = rightInsetPx.coerceAtLeast(0),
                nativeControlsVisible = nativeControlsVisible
            )
        }
    }
}

internal data class WindowChromeEnvironment(
    val platform: WindowPlatform,
    val backend: WindowingBackend,
    val jbrAvailable: Boolean,
    val windowDecorationsSupported: Boolean
)

internal object WindowChromeResolver {

    fun currentEnvironment(): WindowChromeEnvironment {
        val toolkitClassName = runCatching { Toolkit.getDefaultToolkit().javaClass.name }.getOrDefault("")
        return WindowChromeEnvironment(
            platform = detectPlatform(System.getProperty("os.name").orEmpty()),
            backend = detectBackend(toolkitClassName),
            jbrAvailable = runCatching { JBR.isAvailable() }.getOrDefault(false),
            windowDecorationsSupported = runCatching { JBR.isWindowDecorationsSupported() }.getOrDefault(false)
        )
    }

    fun resolve(environment: WindowChromeEnvironment): WindowChromeMode {
        if (!environment.jbrAvailable || !environment.windowDecorationsSupported) {
            return WindowChromeMode.NATIVE_SYSTEM
        }

        return when (environment.platform) {
            WindowPlatform.WINDOWS,
            WindowPlatform.MACOS -> WindowChromeMode.JBR_CUSTOM_TITLEBAR
            WindowPlatform.LINUX,
            WindowPlatform.OTHER -> WindowChromeMode.NATIVE_SYSTEM
        }
    }

    internal fun detectPlatform(osName: String): WindowPlatform {
        val normalized = osName.lowercase(Locale.ROOT)
        return when {
            normalized.contains("win") -> WindowPlatform.WINDOWS
            normalized.contains("mac") || normalized.contains("darwin") -> WindowPlatform.MACOS
            normalized.contains("linux") -> WindowPlatform.LINUX
            else -> WindowPlatform.OTHER
        }
    }

    internal fun detectBackend(toolkitClassName: String): WindowingBackend {
        return when {
            toolkitClassName.contains("XToolkit") -> WindowingBackend.X11
            toolkitClassName.contains("WLToolkit") || toolkitClassName.contains("Wayland") -> WindowingBackend.WAYLAND
            else -> WindowingBackend.UNKNOWN
        }
    }
}
