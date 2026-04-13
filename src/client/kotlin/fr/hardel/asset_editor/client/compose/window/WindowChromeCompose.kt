package fr.hardel.asset_editor.client.compose.window

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.math.roundToInt

internal val LocalWindowChromeState = compositionLocalOf { WindowChromeState.nativeSystem() }
internal val LocalTitleBarClientAreaRegistry = compositionLocalOf { TitleBarClientAreaRegistry.Disabled }

internal fun Modifier.windowChromeClientArea(id: String): Modifier = composed {
    val chromeState = LocalWindowChromeState.current
    val registry = LocalTitleBarClientAreaRegistry.current

    DisposableEffect(id, chromeState.mode, registry) {
        if (chromeState.mode != WindowChromeMode.JBR_CUSTOM_TITLEBAR) {
            registry.removeRegion(id)
        }
        onDispose {
            registry.removeRegion(id)
        }
    }

    onGloballyPositioned { coordinates ->
        if (chromeState.mode != WindowChromeMode.JBR_CUSTOM_TITLEBAR) {
            registry.removeRegion(id)
            return@onGloballyPositioned
        }

        val position = coordinates.positionInWindow()
        registry.updateRegion(
            id,
            WindowClientRect(
                x = position.x.roundToInt(),
                y = position.y.roundToInt(),
                width = coordinates.size.width,
                height = coordinates.size.height
            )
        )
    }
}
