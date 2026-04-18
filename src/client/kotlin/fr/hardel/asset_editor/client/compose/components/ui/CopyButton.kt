package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import kotlinx.coroutines.delay
import net.minecraft.resources.Identifier
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private val COPY_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/copy.svg")

private const val CHECK_DRAW_MS = 420
private const val CONTENT_FADE_MS = 180
private const val SUCCESS_HOLD_MS = 1200L

@Composable
fun CopyButton(
    textProvider: () -> String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    var success by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(success) {
        if (success) {
            checkProgress.snapTo(0f)
            checkProgress.animateTo(1f, tween(CHECK_DRAW_MS, easing = StudioMotion.Ease))
            delay(SUCCESS_HOLD_MS)
            success = false
        }
    }

    val iconTint by animateColorAsState(
        targetValue = if (isHovered) StudioColors.Zinc300 else StudioColors.Zinc500,
        animationSpec = StudioMotion.hoverSpec(),
        label = "copy-icon-tint"
    )
    val copyAlpha by animateFloatAsState(
        targetValue = if (success) 0f else 1f,
        animationSpec = tween(CONTENT_FADE_MS, easing = StudioMotion.Ease),
        label = "copy-content-alpha"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (success) 1f else 0f,
        animationSpec = tween(CONTENT_FADE_MS, easing = StudioMotion.Ease),
        label = "copy-check-alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(iconSize + 16.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(textProvider()), null)
                success = true
            }
    ) {
        SvgIcon(
            location = COPY_ICON,
            size = iconSize,
            tint = iconTint,
            modifier = Modifier.alpha(copyAlpha)
        )
        AnimatedCheckmark(
            progress = checkProgress.value,
            color = StudioColors.Zinc100,
            size = iconSize,
            modifier = Modifier.alpha(checkAlpha)
        )
    }
}
