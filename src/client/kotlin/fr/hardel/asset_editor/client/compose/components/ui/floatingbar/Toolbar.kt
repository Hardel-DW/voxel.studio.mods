package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import kotlin.math.roundToInt
import kotlin.math.sqrt

// TSX: --island-duration: 400ms; --island-easing: cubic-bezier(0.32, 0.72, 0, 1);
// Same curve and duration for both expand and collapse to match the web version.
private val IslandEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private fun <T> islandExpandSpec(): FiniteAnimationSpec<T> =
    tween(StudioMotion.Medium4, easing = IslandEasing)

private fun <T> islandCollapseSpec(): FiniteAnimationSpec<T> =
    tween(StudioMotion.Medium4, easing = IslandEasing)

private const val SNAP_THRESHOLD = 100f
private val COLLAPSED_HEIGHT = 60.dp

/**
 * Snapshot of the expansion target for the transition.
 * Using a data class as transition target batches all [updateTransition] animations
 * into a single invalidation per frame instead of one per animated value.
 */
private data class IslandTarget(
    val expanded: Boolean,
    val width: Dp,
    val height: Dp
)

@Composable
fun Toolbar(
    floatingBar: FloatingBarState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val expansion = floatingBar.expansion
    val isExpanded = expansion is FloatingBarExpansion.Expanded

    // Single transition batches all animations — one invalidation per frame (Skill §1.5 / §4)
    val target = remember(expansion) {
        when (expansion) {
            is FloatingBarExpansion.Expanded -> IslandTarget(true, expansion.size.width, expansion.size.height)
            FloatingBarExpansion.Collapsed -> IslandTarget(false, 700.dp, COLLAPSED_HEIGHT)
        }
    }
    val transition = updateTransition(target, label = "island")

    val cornerRadius by transition.animateDp(
        transitionSpec = { if (targetState.expanded) islandExpandSpec() else islandCollapseSpec() },
        label = "radius"
    ) { t -> if (t.expanded) 32.dp else 48.dp }
    val animatedHeight by transition.animateDp(
        transitionSpec = { if (targetState.expanded) islandExpandSpec() else islandCollapseSpec() },
        label = "height"
    ) { it.height }
    val animatedWidth by transition.animateDp(
        transitionSpec = { if (targetState.expanded) islandExpandSpec() else islandCollapseSpec() },
        label = "width"
    ) { it.width }
    val contentAlpha by transition.animateFloat(
        transitionSpec = { if (targetState.expanded) islandExpandSpec() else islandCollapseSpec() },
        label = "alpha"
    ) { t -> if (t.expanded) 1f else 0f }
    val contentScale by transition.animateFloat(
        transitionSpec = { if (targetState.expanded) islandExpandSpec() else islandCollapseSpec() },
        label = "scale"
    ) { t -> if (t.expanded) 1f else 0.98f }

    // Cache shape — avoid recreating RoundedCornerShape every frame
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    // Defer heavy content composition by ~2 frames so the size animation starts first
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(32) // ~2 frames at 60fps — lets the morph animation start before composing content
            contentReady = true
        } else {
            contentReady = false
        }
    }

    val focusManager = LocalFocusManager.current
    var isDragging by remember { mutableStateOf(false) }
    var showSnapZone by remember { mutableStateOf(false) }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { focusManager.clearFocus() },
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
                    ) {
                        focusManager.clearFocus()
                        floatingBar.collapse()
                    }
            )
        }

        // Snap zone indicator
        if (isDragging) {
            SnapZoneIndicator(showSnapZone)
        }

        // Dynamic island container
        Box(
            modifier = Modifier
                // Offset in lambda — reads in Layout phase, skips Composition (Skill §1.5)
                .offset { IntOffset(floatingBar.offsetX.roundToInt(), floatingBar.offsetY.roundToInt()) }
                .padding(bottom = 32.dp)
                .height(animatedHeight)
                .then(if (isExpanded) Modifier.width(animatedWidth) else Modifier)
                .shadow(24.dp, shape)
                .clip(shape)
                .background(StudioColors.Zinc950)
                .border(1.dp, StudioColors.Zinc800, shape)
        ) {
            if (isExpanded && expansion is FloatingBarExpansion.Expanded) {
                // Content reads alpha/scale in Draw phase via graphicsLayer (Skill §1.5)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = contentAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                        }
                ) {
                    GrabHandle(
                        modifier = dragModifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        if (contentReady) {
                            expansion.content()
                        }
                    }

                    BottomGrabHandle(dragModifier)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = dragModifier
                        .fillMaxHeight()
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
    val pulseTransition = rememberInfiniteTransition(label = "snapPulse")
    val pulseOpacity by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = StudioMotion.Linear),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Read pulseOpacity in Draw phase only via graphicsLayer (Skill §1.5)
    Box(
        modifier = Modifier
            .padding(bottom = 32.dp)
            .width(300.dp)
            .height(60.dp)
            .graphicsLayer { alpha = if (isNearSnap) pulseOpacity else 0.5f }
    ) {
        // Static Canvas — drawn once, opacity handled by parent graphicsLayer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = StudioColors.Zinc400.copy(alpha = 0.1f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(30.dp.toPx())
            )
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
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .drawBehind {
                    drawRect(if (hovered) StudioColors.Zinc400 else StudioColors.Zinc700)
                }
        )
    }
}

@Composable
private fun BottomGrabHandle(dragModifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = dragModifier
            .fillMaxWidth()
            .height(16.dp)
            .hoverable(interaction)
            .drawBehind {
                drawRect(if (hovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            }
            .pointerHoverIcon(PointerIcon.Hand)
    )
}
