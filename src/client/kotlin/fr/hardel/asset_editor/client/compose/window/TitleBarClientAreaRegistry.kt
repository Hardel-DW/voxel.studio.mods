package fr.hardel.asset_editor.client.compose.window

import com.jetbrains.WindowDecorations
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.util.concurrent.ConcurrentHashMap

internal class TitleBarClientAreaRegistry(
    private val enabled: Boolean = true
) {

    private val regions = ConcurrentHashMap<String, WindowClientRect>()

    fun updateRegion(id: String, region: WindowClientRect) {
        if (!enabled) return
        regions[id] = region
    }

    fun removeRegion(id: String) {
        if (!enabled) return
        regions.remove(id)
    }

    fun clear() {
        if (!enabled) return
        regions.clear()
    }

    fun contains(x: Int, y: Int): Boolean {
        if (!enabled) return false
        return regions.values.any { it.contains(x, y) }
    }

    companion object {
        val Disabled = TitleBarClientAreaRegistry(enabled = false)
    }
}

internal data class WindowClientRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun contains(px: Int, py: Int): Boolean {
        return px >= x && px < x + width && py >= y && py < y + height
    }
}

internal class TitleBarHitTestBridge(
    private val chromeState: () -> WindowChromeState,
    private val customTitleBar: () -> WindowDecorations.CustomTitleBar?,
    private val registry: TitleBarClientAreaRegistry
) : MouseAdapter(), MouseMotionListener {

    fun attach(component: Component) {
        component.addMouseListener(this)
        component.addMouseMotionListener(this)
    }

    override fun mouseClicked(event: MouseEvent) = updateHitTest(event)

    override fun mousePressed(event: MouseEvent) = updateHitTest(event)

    override fun mouseReleased(event: MouseEvent) = updateHitTest(event)

    override fun mouseEntered(event: MouseEvent) = updateHitTest(event)

    override fun mouseDragged(event: MouseEvent) = updateHitTest(event)

    override fun mouseMoved(event: MouseEvent) = updateHitTest(event)

    private fun updateHitTest(event: MouseEvent) {
        val state = chromeState()
        val titleBar = customTitleBar() ?: return
        if (state.mode != WindowChromeMode.JBR_CUSTOM_TITLEBAR) return

        val isClientArea = event.y < 0 ||
            event.y >= state.titleBarHeightPx ||
            registry.contains(event.x, event.y)
        titleBar.forceHitTest(isClientArea)
    }
}
