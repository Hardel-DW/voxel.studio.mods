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
import androidx.compose.ui.graphics.graphicsLayer
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
 */
object StudioMotion {

    // Material 3 duration tokens (ms). Use the named constants, not raw numbers, at call sites.
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

    // Press feedback sits below short1 on purpose — tactile response must feel instantaneous.
    const val Press = 75

    // Material 3 easing curves. `Standard` handles ~90% of cases; reach for the emphasized
    // pair only when motion needs a clear enter-vs-exit asymmetry (routes, dialogs, sections).
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

/** Standard enter transition for AnimatedVisibility/AnimatedContent on tab / section content. */
fun standardTabEnter() = fadeIn(animationSpec = StudioMotion.tabFadeSpec())

fun standardTabExit() = fadeOut(animationSpec = StudioMotion.popupExitSpec())

fun standardCollapseEnter() = fadeIn(animationSpec = StudioMotion.collapseEnterSpec()) +
    expandVertically(animationSpec = StudioMotion.collapseEnterSpec())

fun standardCollapseExit() = fadeOut(animationSpec = StudioMotion.collapseExitSpec()) +
    shrinkVertically(animationSpec = StudioMotion.collapseExitSpec())
