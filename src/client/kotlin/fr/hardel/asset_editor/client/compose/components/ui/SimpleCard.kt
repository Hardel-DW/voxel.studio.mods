package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

@Composable
fun SimpleCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(vertical = 24.dp, horizontal = 32.dp),
    onClick: (() -> Unit)? = null,
    overlay: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val offsetY by animateDpAsState(
        targetValue = if (isHovered) (-4).dp else 0.dp,
        animationSpec = tween(150)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .then(if (onClick != null) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY)
                .clip(RoundedCornerShape(12.dp))
        ) {
            ShineOverlay(opacity = 0.15f)

            overlay?.invoke()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
            ) {
                content()
            }
        }
    }
}
