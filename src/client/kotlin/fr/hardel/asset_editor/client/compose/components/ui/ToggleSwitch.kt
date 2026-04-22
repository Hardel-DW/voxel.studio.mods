package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import fr.hardel.asset_editor.client.compose.StudioMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors

private val SWITCH_WIDTH = 44.dp
private val SWITCH_HEIGHT = 24.dp
private val KNOB_DIAMETER = 20.dp
private val PAD = 2.dp

@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) SWITCH_WIDTH - PAD - KNOB_DIAMETER else PAD,
        animationSpec = StudioMotion.tabFadeSpec()
    )

    val railBrush = if (checked) StudioColors.CheckedRail else Brush.linearGradient(listOf(StudioColors.UncheckedRail, StudioColors.UncheckedRail))

    val knobColor by animateColorAsState(
        targetValue = if (checked) StudioColors.CheckedCircle else StudioColors.UncheckedCircle,
        animationSpec = StudioMotion.tabFadeSpec()
    )

    Box(
        modifier = modifier
            .width(SWITCH_WIDTH)
            .height(SWITCH_HEIGHT)
            .clip(RoundedCornerShape(SWITCH_HEIGHT))
            .background(railBrush)
            .then(
                if (enabled) Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!checked) }
                else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = knobOffset)
                .size(KNOB_DIAMETER)
                .clip(CircleShape)
                .background(knobColor)
        )
    }
}
