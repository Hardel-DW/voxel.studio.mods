package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography

enum class ButtonVariant {
    DEFAULT, BLACK, GHOST, GHOST_BORDER, AURORA, TRANSPARENT, LINK, SHIMMER, PATREON
}

enum class ButtonSize {
    NONE, SQUARE, DEFAULT, SM, LG, XL, ICON
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.DEFAULT,
    size: ButtonSize = ButtonSize.DEFAULT,
    text: String? = null,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = StudioMotion.pressSpec(),
        label = "button-press-scale"
    )

    val height = buttonHeight(size)
    val contentPadding = buttonPadding(size)

    val shape = RoundedCornerShape(12.dp)
    val mutedDefault = !enabled && variant == ButtonVariant.DEFAULT
    val bgModifier = if (mutedDefault) Modifier.background(Color.Transparent)
        else variantBackground(variant, isHovered)
    val borderModifier = if (mutedDefault) Modifier.border(2.dp, StudioColors.Zinc800, shape)
        else variantBorder(variant, isHovered)
    val textColorTarget = if (mutedDefault) StudioColors.Zinc600 else buttonTextColor(variant, isHovered)
    val textColor by animateColorAsState(
        targetValue = textColorTarget,
        animationSpec = StudioMotion.hoverSpec(),
        label = "button-text"
    )

    val hoverAlpha = when {
        mutedDefault -> 1f
        !enabled -> 0.5f
        variant == ButtonVariant.SHIMMER && isHovered -> 0.75f
        variant == ButtonVariant.PATREON && isHovered -> 0.9f
        else -> 1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(if (size == ButtonSize.ICON) Modifier.size(height) else Modifier.height(height))
            .clip(shape)
            .then(bgModifier)
            .then(borderModifier)
            .alpha(hoverAlpha)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .then(buttonEffects(variant))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding)
        ) {
            icon?.invoke()
            if (text != null) {
                Text(
                    text = text,
                    style = StudioTypography.medium(14),
                    color = textColor
                )
            }
        }
    }
}

private fun buttonTextColor(variant: ButtonVariant, hovered: Boolean): Color = when (variant) {
    ButtonVariant.DEFAULT -> StudioColors.Zinc800
    ButtonVariant.BLACK -> StudioColors.Zinc200
    ButtonVariant.GHOST -> if (hovered) StudioColors.Zinc800 else StudioColors.Zinc200
    ButtonVariant.GHOST_BORDER -> if (hovered) StudioColors.Zinc100 else StudioColors.Zinc200
    ButtonVariant.AURORA -> if (hovered) StudioColors.Zinc100 else StudioColors.Zinc400
    ButtonVariant.TRANSPARENT -> StudioColors.Zinc200
    ButtonVariant.LINK -> if (hovered) Color.White else StudioColors.Zinc400
    ButtonVariant.SHIMMER -> StudioColors.Background
    ButtonVariant.PATREON -> Color.White
}

private fun buttonHeight(size: ButtonSize): Dp = when (size) {
    ButtonSize.SM -> 36.dp
    ButtonSize.LG -> 40.dp
    ButtonSize.XL -> 48.dp
    ButtonSize.ICON, ButtonSize.SQUARE -> 40.dp
    else -> 40.dp
}

private fun buttonPadding(size: ButtonSize) = when (size) {
    ButtonSize.SM -> PaddingValues(horizontal = 12.dp)
    ButtonSize.LG -> PaddingValues(horizontal = 32.dp)
    ButtonSize.XL -> PaddingValues(horizontal = 40.dp)
    ButtonSize.ICON, ButtonSize.SQUARE -> PaddingValues(8.dp)
    else -> PaddingValues(horizontal = 16.dp)
}

@Composable
private fun variantBackground(variant: ButtonVariant, hovered: Boolean): Modifier {
    val targetColor = when (variant) {
        ButtonVariant.DEFAULT -> if (hovered) StudioColors.Zinc300 else StudioColors.Zinc200
        ButtonVariant.BLACK -> if (hovered) StudioColors.Zinc900 else Color.Black
        ButtonVariant.GHOST -> if (hovered) StudioColors.Zinc200 else Color.Transparent
        ButtonVariant.GHOST_BORDER, ButtonVariant.LINK, ButtonVariant.AURORA -> Color.Transparent
        ButtonVariant.TRANSPARENT -> if (hovered) Color.White.copy(alpha = 0.1f) else Color.Transparent
        ButtonVariant.SHIMMER -> StudioColors.Zinc100
        ButtonVariant.PATREON -> StudioColors.Orange700
    }
    val animated by animateColorAsState(
        targetValue = targetColor,
        animationSpec = StudioMotion.hoverSpec(),
        label = "button-bg"
    )
    return if (variant == ButtonVariant.AURORA) {
        // Keeps its original gradient; no animation needed.
        Modifier.background(
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    StudioColors.Zinc700.copy(alpha = 0.3f)
                )
            )
        )
    } else {
        Modifier.background(animated)
    }
}

@Composable
private fun variantBorder(variant: ButtonVariant, hovered: Boolean): Modifier {
    val shape = RoundedCornerShape(12.dp)
    val targetColor = when (variant) {
        ButtonVariant.DEFAULT, ButtonVariant.BLACK, ButtonVariant.GHOST, ButtonVariant.TRANSPARENT ->
            StudioColors.Zinc500
        ButtonVariant.GHOST_BORDER -> if (hovered) StudioColors.Zinc700 else StudioColors.Zinc900
        ButtonVariant.AURORA -> if (hovered) StudioColors.Zinc800 else StudioColors.Zinc900
        else -> return Modifier
    }
    val animated by animateColorAsState(
        targetValue = targetColor,
        animationSpec = StudioMotion.hoverSpec(),
        label = "button-border"
    )
    return Modifier.border(2.dp, animated, shape)
}

@Composable
private fun buttonEffects(variant: ButtonVariant): Modifier {
    if (variant != ButtonVariant.SHIMMER && variant != ButtonVariant.PATREON) {
        return Modifier
    }

    val infiniteTransition = rememberInfiniteTransition()
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    return Modifier.drawWithContent {
        drawContent()
        if (variant == ButtonVariant.SHIMMER) {
            val strokeWidth = 1.dp.toPx()
            drawLine(
                color = StudioColors.Zinc900,
                start = Offset(0f, strokeWidth / 2f),
                end = Offset(size.width, strokeWidth / 2f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = StudioColors.Zinc900,
                start = Offset(strokeWidth / 2f, 0f),
                end = Offset(strokeWidth / 2f, size.height),
                strokeWidth = strokeWidth
            )
        }
        val stripeWidth = size.width * 0.4f
        val x = shimmerOffset * size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                startX = x,
                endX = x + stripeWidth
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height)
        )
    }
}
