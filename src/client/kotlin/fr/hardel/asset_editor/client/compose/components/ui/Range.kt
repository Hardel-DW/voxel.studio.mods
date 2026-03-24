package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import kotlin.math.roundToInt

private val RANGE_HEIGHT = 24.dp
private val RANGE_TRACK_HEIGHT = 8.dp
private val RANGE_VISUAL_THUMB_SIZE = 24.dp
private val RANGE_VISUAL_THUMB_BORDER = 3.dp
private val RANGE_GESTURE_THUMB_RADIUS = 10.dp
private val RANGE_ACTIVE_SHAPE = RoundedCornerShape(
    topStart = RANGE_HEIGHT / 2,
    bottomStart = RANGE_HEIGHT / 2,
    topEnd = 0.dp,
    bottomEnd = 0.dp
)

@Composable
fun Range(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    lockText: String? = null
) {
    val displayLabel = if (locked && lockText != null) lockText else label
    val coercedValue = value.coerceIn(min, max)
    val safeStep = step.coerceAtLeast(1)
    val valueSpan = (max - min).coerceAtLeast(0)
    val steps = if (valueSpan == 0) 0 else ((valueSpan / safeStep) - 1).coerceAtLeast(0)

    var sliderValue by remember { mutableFloatStateOf(coercedValue.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var sliderWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(coercedValue, dragging) {
        if (!dragging) {
            sliderValue = coercedValue.toFloat()
        }
    }

    val displayedValue = if (dragging) {
        val stepIndex = ((sliderValue - min) / safeStep.toFloat()).roundToInt()
        (min + stepIndex * safeStep).coerceIn(min, max)
    } else {
        coercedValue
    }

    val density = LocalDensity.current
    val gestureThumbRadiusPx = with(density) { RANGE_GESTURE_THUMB_RADIUS.toPx() }
    val visualThumbRadiusPx = with(density) { (RANGE_VISUAL_THUMB_SIZE / 2).toPx() }
    val fraction = if (valueSpan == 0) 0f else (displayedValue - min).toFloat() / valueSpan.toFloat()
    val usableWidthPx = (sliderWidthPx - gestureThumbRadiusPx * 2f).coerceAtLeast(0f)
    val thumbCenterPx = gestureThumbRadiusPx + usableWidthPx * fraction
    val activeWidth = with(density) {
        if (displayedValue <= min) 0.dp else thumbCenterPx.toDp()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (locked) 0.5f else 1f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        ) {
            Text(
                text = displayLabel,
                style = VoxelTypography.medium(13),
                color = VoxelColors.Zinc400,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = displayedValue.toString(),
                style = VoxelTypography.medium(13),
                color = VoxelColors.Zinc400,
                textAlign = TextAlign.End
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RANGE_HEIGHT)
                .onSizeChanged { sliderWidthPx = it.width.toFloat() }
                .then(if (!locked) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(RANGE_TRACK_HEIGHT)
                    .background(VoxelColors.Tertiary, CircleShape)
            )

            if (activeWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(activeWidth)
                        .height(RANGE_HEIGHT)
                        .background(VoxelColors.Primary, RANGE_ACTIVE_SHAPE)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset(
                            x = (thumbCenterPx - visualThumbRadiusPx).roundToInt(),
                            y = 0
                        )
                    }
                    .size(RANGE_VISUAL_THUMB_SIZE)
                    .border(RANGE_VISUAL_THUMB_BORDER, VoxelColors.Primary, CircleShape)
                    .background(Color.Black, CircleShape)
            )

            Slider(
                value = sliderValue,
                onValueChange = { raw ->
                    dragging = true
                    sliderValue = raw
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = steps,
                enabled = !locked,
                onValueChangeFinished = {
                    dragging = false
                    val snapped = if (valueSpan == 0) {
                        min
                    } else {
                        val stepIndex = ((sliderValue - min) / safeStep.toFloat()).roundToInt()
                        (min + stepIndex * safeStep).coerceIn(min, max)
                    }
                    sliderValue = snapped.toFloat()
                    if (snapped != coercedValue) {
                        onValueChange(snapped)
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RANGE_HEIGHT)
                    .alpha(0.01f)
            )
        }
    }
}
