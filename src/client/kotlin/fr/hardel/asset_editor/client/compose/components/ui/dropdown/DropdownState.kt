package fr.hardel.asset_editor.client.compose.components.ui.dropdown

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.resources.Identifier

private const val SUBMENU_CLOSE_DELAY = 150L
private const val GRACE_TIMEOUT = 300L

internal val CHEVRON_DOWN_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
internal val CHEVRON_RIGHT_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-right.svg")
internal val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check-simple.svg")

internal val contentShape = RoundedCornerShape(8.dp)
internal val itemShape = RoundedCornerShape(6.dp)
internal val selectTriggerShape = RoundedCornerShape(20.dp)

enum class DropdownItemVariant { DEFAULT, DESTRUCTIVE }

enum class DropdownMenuSide { BOTTOM, TOP }

// ── Menu state ──────────────────────────────────────────────────────────────

class DropdownMenuState {
    var expanded by mutableStateOf(false)
    var triggerWidthPx by mutableIntStateOf(0)

    fun open() { expanded = true }
    fun close() { expanded = false }
    fun toggle() { expanded = !expanded }
}

@Composable
fun rememberDropdownMenuState(): DropdownMenuState = remember { DropdownMenuState() }

internal val LocalDropdownMenuState = compositionLocalOf<DropdownMenuState> {
    error("DropdownMenu not found in composition")
}

internal val LocalDropdownMenuClose = compositionLocalOf<() -> Unit> {
    error("DropdownMenu not found in composition")
}

// ── Sub-menu state ──────────────────────────────────────────────────────────

internal class DropdownSubMenuState {
    var expanded by mutableStateOf(false)
    private var triggerHovered = false
    private var contentHovered = false
    private var inSafeTriangle = false
    private var closeJob: Job? = null

    fun setTriggerHovered(value: Boolean, scope: CoroutineScope) {
        triggerHovered = value
        refresh(scope)
    }

    fun setContentHovered(value: Boolean, scope: CoroutineScope) {
        contentHovered = value
        if (!value) inSafeTriangle = false
        refresh(scope)
    }

    fun setInSafeTriangle(value: Boolean, scope: CoroutineScope) {
        inSafeTriangle = value
        refresh(scope)
    }

    fun open() {
        closeJob?.cancel()
        closeJob = null
        expanded = true
    }

    fun close() {
        closeJob?.cancel()
        closeJob = null
        triggerHovered = false
        contentHovered = false
        inSafeTriangle = false
        expanded = false
    }

    private fun refresh(scope: CoroutineScope) {
        if (triggerHovered || contentHovered || inSafeTriangle) {
            closeJob?.cancel()
            closeJob = null
            expanded = true
        } else if (expanded) {
            closeJob?.cancel()
            closeJob = scope.launch {
                delay(SUBMENU_CLOSE_DELAY)
                expanded = false
                closeJob = null
            }
        }
    }
}

internal val LocalDropdownSubMenuState = compositionLocalOf<DropdownSubMenuState?> { null }
internal val LocalOpenSubMenu = compositionLocalOf<MutableState<DropdownSubMenuState?>?> { null }

// ── Safe triangle (Ben Kamens algorithm) ────────────────────────────────────
// https://bjk5.com/post/44698559168/breaking-down-amazons-mega-dropdown

internal class SafeTriangleTracker {
    var cursorWindow by mutableStateOf<Offset?>(null)
    var submenuWindowBounds by mutableStateOf<Rect?>(null)
    var activeSubState by mutableStateOf<DropdownSubMenuState?>(null)
    var graceApex by mutableStateOf<Offset?>(null)
    private var graceJob: Job? = null

    fun onSubMenuOpened(state: DropdownSubMenuState) {
        activeSubState = state
    }

    fun onSubMenuBounds(bounds: Rect, state: DropdownSubMenuState) {
        if (activeSubState === state) submenuWindowBounds = bounds
    }

    fun onSubMenuClosed(state: DropdownSubMenuState, scope: CoroutineScope) {
        if (activeSubState === state) {
            submenuWindowBounds = null
            activeSubState = null
            clearGrace(scope)
        }
    }

    fun onTriggerLeave(scope: CoroutineScope) {
        val cursor = cursorWindow ?: return
        submenuWindowBounds ?: return
        graceApex = cursor
        graceJob?.cancel()
        graceJob = scope.launch {
            delay(GRACE_TIMEOUT)
            clearGrace(scope)
        }
    }

    fun clearGrace(scope: CoroutineScope) {
        graceJob?.cancel()
        graceJob = null
        graceApex = null
        activeSubState?.setInSafeTriangle(false, scope)
    }

    fun blocksOpeningOf(state: DropdownSubMenuState): Boolean {
        val apex = graceApex ?: return false
        val bounds = submenuWindowBounds ?: return false
        val cursor = cursorWindow ?: return false
        return activeSubState !== state && pointInSafeTriangle(cursor, apex, bounds)
    }

    fun onCursor(windowPos: Offset, scope: CoroutineScope) {
        cursorWindow = windowPos
        val sub = activeSubState ?: return
        val apex = graceApex
        val bounds = submenuWindowBounds
        if (apex != null && bounds != null) {
            val inside = pointInSafeTriangle(windowPos, apex, bounds)
            sub.setInSafeTriangle(inside, scope)
            if (!inside) clearGrace(scope)
        }
    }
}

internal val LocalSafeTriangleTracker = compositionLocalOf<SafeTriangleTracker?> { null }

private fun pointInSafeTriangle(p: Offset, apex: Offset, submenu: Rect): Boolean {
    val edgeX = if (submenu.left >= apex.x) submenu.left else submenu.right
    return pointInTriangle(p, apex, Offset(edgeX, submenu.top), Offset(edgeX, submenu.bottom))
}

private fun pointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
    val s1 = triSign(p, a, b)
    val s2 = triSign(p, b, c)
    val s3 = triSign(p, c, a)
    return !((s1 < 0f || s2 < 0f || s3 < 0f) && (s1 > 0f || s2 > 0f || s3 > 0f))
}

private fun triSign(p1: Offset, p2: Offset, p3: Offset): Float =
    (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)

internal fun DrawScope.drawSafeTriangle(tracker: SafeTriangleTracker, coords: LayoutCoordinates?) {
    val bounds = tracker.submenuWindowBounds ?: return
    val apex = tracker.graceApex ?: return
    val origin = coords?.localToWindow(Offset.Zero) ?: return
    val edgeX = if (bounds.left >= apex.x) bounds.left else bounds.right
    val ax = apex.x - origin.x
    val ay = apex.y - origin.y
    val path = Path().apply {
        moveTo(ax, ay)
        lineTo(edgeX - origin.x, bounds.top - origin.y)
        lineTo(edgeX - origin.x, bounds.bottom - origin.y)
        close()
    }
    drawPath(path, Color(0x3300FF00))
    drawPath(path, Color(0xFF00FF00), style = Stroke(width = 2f))
    drawCircle(Color.Red, 4f, Offset(ax, ay))
}

// ── Radio group ─────────────────────────────────────────────────────────────

internal class RadioGroupState(val value: String, val onValueChange: (String) -> Unit)

internal val LocalRadioGroupState = compositionLocalOf<RadioGroupState?> { null }
