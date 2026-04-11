package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ANIM_DURATION = 400
private val ISLAND_EASING = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private fun <T> islandTween() = tween<T>(ANIM_DURATION, easing = ISLAND_EASING)
private const val SNAP_THRESHOLD = 100f

private val COLLAPSED_HEIGHT = 60.dp

@Composable
fun Toolbar(
    floatingBar: FloatingBarState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val expansion = floatingBar.expansion
    val isExpanded = expansion is FloatingBarExpansion.Expanded

    // Animated border-radius: 48dp (collapsed rounded-4xl) → 32dp (expanded rounded-3xl)
    val cornerRadius by animateDpAsState(
        if (isExpanded) 32.dp else 48.dp, islandTween()
    )
    val shape = RoundedCornerShape(cornerRadius)

    // Animated size — the core dynamic island morph effect
    // Height always animates between collapsed (60dp) and expanded target
    // Width only animates when expanded (collapsed = wrap-content, not animatable)
    val targetHeight = if (isExpanded && expansion is FloatingBarExpansion.Expanded) expansion.size.height else COLLAPSED_HEIGHT
    val animatedHeight by animateDpAsState(targetHeight, islandTween())
    val expandedWidth = if (expansion is FloatingBarExpansion.Expanded) expansion.size.width else 700.dp
    val animatedWidth by animateDpAsState(expandedWidth, islandTween())

    // Content animations: opacity 0→1, scale 0.98→1 (island-content-in)
    val contentAlpha by animateFloatAsState(if (isExpanded) 1f else 0f, islandTween())
    val contentScale by animateFloatAsState(if (isExpanded) 1f else 0.98f, islandTween())

    var isDragging by remember { mutableStateOf(false) }
    var showSnapZone by remember { mutableStateOf(false) }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                floatingBar.offsetX += dragAmount.x
                floatingBar.offsetY += dragAmount.y
                isDragging = true
                val distance = sqrt(floatingBar.offsetX * floatingBar.offsetX + floatingBar.offsetY * floatingBar.offsetY)
                showSnapZone = distance < SNAP_THRESHOLD
            },
            onDragEnd = {
                val distance = sqrt(floatingBar.offsetX * floatingBar.offsetX + floatingBar.offsetY * floatingBar.offsetY)
                if (distance < SNAP_THRESHOLD) {
                    floatingBar.resetPosition()
                }
                isDragging = false
                showSnapZone = false
            },
            onDragCancel = {
                isDragging = false
                showSnapZone = false
            }
        )
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier.fillMaxSize()
    ) {
        // Click-outside overlay — collapses toolbar when clicking outside (TSX: useClickOutside)
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { floatingBar.collapse() }
            )
        }

        // Snap zone indicator — border-2 border-dashed border-zinc-400/50 bg-zinc-400/10 rounded-full animate-pulse
        if (isDragging) {
            SnapZoneIndicator(showSnapZone)
        }

        // Dynamic island container
        Box(
            modifier = Modifier
                .offset { IntOffset(floatingBar.offsetX.roundToInt(), floatingBar.offsetY.roundToInt()) }
                .padding(bottom = 32.dp)
                .height(animatedHeight)
                .then(
                    if (isExpanded) Modifier.width(animatedWidth) else Modifier
                )
                .shadow(24.dp, shape)
                .clip(shape)
                .background(StudioColors.Zinc950.copy(alpha = 0.5f))
                .border(1.dp, StudioColors.Zinc800, shape)
        ) {
            if (isExpanded && expansion is FloatingBarExpansion.Expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = contentAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                        }
                ) {
                    // Top grab handle — h-6, cursor-move
                    GrabHandle(
                        modifier = dragModifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    )

                    // Content — px-6 pb-2
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        expansion.content()
                    }

                    // Bottom grab handle — h-4, cursor-move, hover:bg-white/5
                    BottomGrabHandle(dragModifier)
                }
            } else {
                // Collapsed — p-2, gap-4, cursor-move, items-center h-full
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = dragModifier
                        .fillMaxHeight() // h-full — ensures vertical centering works
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SnapZoneIndicator(isNearSnap: Boolean) {
    // TSX: animate-pulse = opacity pulse 0.5→1.0 over 2s cubic-bezier(0.4, 0, 0.6, 1)
    val pulseTransition = rememberInfiniteTransition()
    val pulseOpacity by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .padding(bottom = 32.dp)
            .width(300.dp)
            .height(60.dp)
            .graphicsLayer { alpha = if (isNearSnap) pulseOpacity else 0.5f }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // bg-zinc-400/10
            drawRoundRect(
                color = StudioColors.Zinc400.copy(alpha = 0.1f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(30.dp.toPx())
            )
            // border-2 border-dashed border-zinc-400/50
            drawRoundRect(
                color = StudioColors.Zinc400.copy(alpha = 0.5f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(30.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            )
        }
    }
}

@Composable
private fun GrabHandle(modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.hoverable(interaction)
    ) {
        // w-10 h-1 bg-zinc-700 rounded-full group-hover:bg-zinc-400 transition-colors
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (hovered) StudioColors.Zinc400 else StudioColors.Zinc700)
        )
    }
}

@Composable
private fun BottomGrabHandle(dragModifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // h-4 cursor-move shrink-0 rounded-b-3xl hover:bg-white/5 transition-colors
    Box(
        modifier = dragModifier
            .fillMaxWidth()
            .height(16.dp)
            .hoverable(interaction)
            .background(if (hovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand)
    )
}
