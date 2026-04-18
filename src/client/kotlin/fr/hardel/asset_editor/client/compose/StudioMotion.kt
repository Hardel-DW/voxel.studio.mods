package fr.hardel.asset_editor.client.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
 * Pick a duration bucket first, then a spec. Prefer [StudioMotion.Ease] everywhere unless
 * you have a specific reason — consistency matters more than micro-tuning.
 *
 * Where to use what
 * -----------------
 * - Hover / press / color fades on interactive elements -> [hoverSpec] (Quick)
 * - Popup / dropdown / menu enter/exit -> [popupEnterSpec] / [popupExitSpec]
 * - Page-level enter (route, overview pages) -> [pageEnterSpec] + [PageEnterAnimation]
 * - Tab content swap -> [tabFadeSpec]
 * - Collapsing sections (advanced options) -> [collapseEnterSpec] / [collapseExitSpec]
 */
object StudioMotion {

    // Durations (ms)
    const val Instant = 75
    const val Quick = 120
    const val Standard = 180
    const val Emphasized = 300

    // Easings
    val Ease: Easing = FastOutSlowInEasing
    val EaseOut: Easing = LinearOutSlowInEasing
    val EaseInOut: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    fun <T> hoverSpec(): TweenSpec<T> = tween(Quick, easing = Ease)
    fun <T> pressSpec(): TweenSpec<T> = tween(Instant, easing = Ease)
    fun <T> popupEnterSpec(): TweenSpec<T> = tween(Quick, easing = Ease)
    fun <T> popupExitSpec(): TweenSpec<T> = tween(Instant, easing = Ease)
    fun <T> pageEnterSpec(): TweenSpec<T> = tween(Standard, easing = Ease)
    fun <T> tabFadeSpec(): TweenSpec<T> = tween(Standard, easing = Ease)
    fun <T> collapseEnterSpec(): TweenSpec<T> = tween(Standard, easing = Ease)
    fun <T> collapseExitSpec(): TweenSpec<T> = tween(Quick, easing = Ease)
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
                translationY = (1f - p) * 4.dp.toPx()
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
