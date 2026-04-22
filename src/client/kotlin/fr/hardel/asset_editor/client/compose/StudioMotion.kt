package fr.hardel.asset_editor.client.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for motion: durations, easings, and reusable animation specs.
 *
 * Values track the Material 3 2026 motion specification (m3.material.io/styles/motion):
 * duration tokens in the short / medium / long tiers, and three easing curves:
 * - [Standard] for everyday UI — colour fades, hover, toggle-like state changes.
 * - [EmphasizedDecelerate] for elements entering the screen (arrive fast, settle).
 * - [EmphasizedAccelerate] for elements leaving the screen (ease out, exit fast).
 *
 * Rules of thumb (do not micro-tune without a reason):
 * - Hover / tint fades → [hoverSpec] (short3 = 150ms, standard).
 * - Press feedback    → [pressSpec] (~short1-2 = 75ms, standard).
 * - Popup / menu enter→ [popupEnterSpec] (short4 + standard-decelerate).
 * - Popup exit        → [popupExitSpec]  (short2 + standard-accelerate, always faster than enter).
 * - Section collapse  → [collapseEnterSpec] / [collapseExitSpec].
 * - Route / page enter→ [pageEnterSpec] + [PageEnterAnimation] (medium4, emphasized-decelerate).
 * - Tab content swap  → [tabFadeSpec].
 *
 * Material 3 duration tokens (ms). Use the named constants, not raw numbers, at call sites.
 * Material 3 easing curves. `Standard` handles ~90% of cases; reach for the emphasized
 */
object StudioMotion {
    const val Short1 = 50
    const val Short2 = 100
    const val Short3 = 150
    const val Short4 = 200
    const val Medium1 = 250
    const val Medium2 = 300
    const val Medium3 = 350
    const val Medium4 = 400
    const val Long1 = 450
    const val Long2 = 500
    const val Press = 75

    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Linear: Easing = LinearEasing

    fun <T> hoverSpec(): TweenSpec<T> = tween(Short3, easing = Standard)
    fun <T> pressSpec(): TweenSpec<T> = tween(Press, easing = Standard)

    fun <T> popupEnterSpec(): TweenSpec<T> = tween(Short4, easing = EmphasizedDecelerate)
    fun <T> popupExitSpec(): TweenSpec<T> = tween(Short2, easing = EmphasizedAccelerate)

    fun <T> pageEnterSpec(): TweenSpec<T> = tween(Medium4, easing = EmphasizedDecelerate)

    fun <T> tabFadeSpec(): TweenSpec<T> = tween(Short4, easing = Standard)

    fun <T> collapseEnterSpec(): TweenSpec<T> = tween(Medium2, easing = EmphasizedDecelerate)
    fun <T> collapseExitSpec(): TweenSpec<T> = tween(Short4, easing = EmphasizedAccelerate)

    /** Checkmark / success path tween — community convention (Framer): ~350ms decelerate. */
    fun <T> checkmarkSpec(): TweenSpec<T> = tween(Medium3, easing = EmphasizedDecelerate)
}

/**
 * Wraps route or section content with the standard page enter animation: a short fade + small
 * upward translation. Call this as the outermost layout in a route to normalize first-paint.
 *
 * Safe to nest inside scrollable containers — it only affects the first composition.
 */
@Composable
fun PageEnterAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, StudioMotion.pageEnterSpec())
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                alpha = 0.7f + 0.3f * p
                translationY = (1f - p) * 6.dp.toPx()
            }
    ) { content() }
}

/**
 * Standard popup/dropdown enter transform: fade + subtle scale + optional upward slide.
 * Apply to any element that needs the standard appear animation driven by an external [progress] (0→1).
 * Works for both enter (0→1) and exit (1→0) when paired with [StudioMotion.popupExitSpec].
 */
fun Modifier.popupEnterTransform(
    progress: Float,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    translateY: Dp = 0.dp
): Modifier = graphicsLayer {
    alpha = progress
    val scale = 0.96f + 0.04f * progress
    scaleX = scale
    scaleY = scale
    if (translateY.value > 0f) translationY = (1f - progress) * translateY.toPx()
    this.transformOrigin = transformOrigin
}

/**
 * Convenience wrapper: manages its own progress and applies [popupEnterTransform].
 * Use for simple popups that only need an enter animation. For popups with exit
 * animation or a backdrop layer, use [popupEnterTransform] directly with your own [Animatable].
 */
@Composable
fun PopupEnterAnimation(
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, StudioMotion.popupEnterSpec())
    }
    Box(
        modifier = modifier.popupEnterTransform(progress.value, transformOrigin, translateY = 8.dp)
    ) { content() }
}

/**
 * Slide-in enter transform: fade + directional translation (no scale).
 * Use for toasts, sidebar items, or anything that slides into view.
 */
fun Modifier.slideEnterTransform(
    progress: Float,
    translateY: Dp = 0.dp,
    translateX: Dp = 0.dp
): Modifier = graphicsLayer {
    alpha = progress
    if (translateY.value != 0f) translationY = (1f - progress) * translateY.toPx()
    if (translateX.value != 0f) translationX = (1f - progress) * translateX.toPx()
}

/** Disabled / enabled alpha toggle via graphicsLayer (no recomposition). */
fun Modifier.enabledAlpha(enabled: Boolean): Modifier =
    graphicsLayer { alpha = if (enabled) 1f else 0.5f }

/** Uniform scale via graphicsLayer — use for press feedback driven by an animated Float. */
fun Modifier.pressScale(scale: Float): Modifier =
    graphicsLayer { scaleX = scale; scaleY = scale }

fun standardTabEnter() = fadeIn(animationSpec = StudioMotion.tabFadeSpec())

fun standardTabExit() = fadeOut(animationSpec = StudioMotion.popupExitSpec())

fun standardCollapseEnter() = fadeIn(animationSpec = StudioMotion.collapseEnterSpec()) +
        expandVertically(animationSpec = StudioMotion.collapseEnterSpec())

fun standardCollapseExit() = fadeOut(animationSpec = StudioMotion.collapseExitSpec()) +
        shrinkVertically(animationSpec = StudioMotion.collapseExitSpec())
