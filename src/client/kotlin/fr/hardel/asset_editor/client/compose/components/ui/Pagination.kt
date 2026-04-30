package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.resources.Identifier

private const val VISIBLE = 5
private const val DRAG_PIXELS_PER_PAGE = 36f

private val CHEVRON_LEFT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-left.svg")
private val CHEVRON_RIGHT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-right.svg")
private val CHEVRONS_LEFT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevrons-left.svg")
private val CHEVRONS_RIGHT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevrons-right.svg")

@Composable
fun Pagination(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    val count = minOf(totalPages, VISIBLE)
    val half = count / 2
    val start = maxOf(0, minOf(currentPage - half, totalPages - count))

    val currentRef = rememberUpdatedState(currentPage)
    val totalRef = rememberUpdatedState(totalPages)
    val onChangeRef = rememberUpdatedState(onPageChange)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.pointerInput(Unit) {
            var accumulated = 0f
            detectHorizontalDragGestures(
                onDragStart = { accumulated = 0f },
                onDragEnd = { accumulated = 0f },
                onDragCancel = { accumulated = 0f }
            ) { change, dragAmount ->
                accumulated += dragAmount
                val steps = (accumulated / DRAG_PIXELS_PER_PAGE).toInt()
                if (steps != 0) {
                    accumulated -= steps * DRAG_PIXELS_PER_PAGE
                    val next = (currentRef.value + steps).coerceIn(0, totalRef.value - 1)
                    if (next != currentRef.value) {
                        onChangeRef.value(next)
                        change.consume()
                    }
                }
            }
        }
    ) {
        IconNavButton(CHEVRONS_LEFT, enabled = currentPage > 0) { onPageChange(0) }
        IconNavButton(CHEVRON_LEFT, enabled = currentPage > 0) { onPageChange(currentPage - 1) }

        for (i in 0 until count) {
            val page = start + i
            PageButton(page, isCurrent = page == currentPage) { onPageChange(page) }
        }

        IconNavButton(CHEVRON_RIGHT, enabled = currentPage < totalPages - 1) { onPageChange(currentPage + 1) }
        IconNavButton(CHEVRONS_RIGHT, enabled = currentPage < totalPages - 1) { onPageChange(totalPages - 1) }
    }
}

@Composable
private fun PageButton(page: Int, isCurrent: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val targetBg = when {
        isCurrent -> StudioColors.Zinc800
        hovered -> StudioColors.Zinc900.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    val bg by animateColorAsState(targetBg, animationSpec = StudioMotion.hoverSpec(), label = "page-bg")
    val targetText = when {
        isCurrent -> StudioColors.Zinc100
        hovered -> StudioColors.Zinc300
        else -> StudioColors.Zinc500
    }
    val textColor by animateColorAsState(targetText, animationSpec = StudioMotion.hoverSpec(), label = "page-text")
    val scale by animateFloatAsState(if (isCurrent) 1.05f else 1f, animationSpec = StudioMotion.pressSpec(), label = "page-scale")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Text(
            text = (page + 1).toString(),
            style = StudioTypography.medium(12),
            color = textColor
        )
    }
}

@Composable
private fun IconNavButton(icon: Identifier, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val targetTint = when {
        !enabled -> StudioColors.Zinc700
        hovered -> StudioColors.Zinc100
        else -> StudioColors.Zinc400
    }
    val tint by animateColorAsState(targetTint, animationSpec = StudioMotion.hoverSpec(), label = "nav-tint")
    val targetBg = if (hovered && enabled) StudioColors.Zinc800.copy(alpha = 0.5f) else Color.Transparent
    val bg by animateColorAsState(targetBg, animationSpec = StudioMotion.hoverSpec(), label = "nav-bg")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .hoverable(interaction, enabled = enabled)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .then(
                if (enabled) Modifier.clickable(interactionSource = interaction, indication = null) { onClick() }
                else Modifier
            )
    ) {
        SvgIcon(icon, 12.dp, tint)
    }
}
