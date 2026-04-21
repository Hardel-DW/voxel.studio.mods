package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import kotlinx.coroutines.launch

@Composable
fun AnimatedTabs(
    options: LinkedHashMap<String, String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tabPositions = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    var measured by remember { mutableStateOf(false) }

    val activePos = tabPositions[selectedValue]

    LaunchedEffect(activePos) {
        if (activePos == null) return@LaunchedEffect
        if (!measured) {
            measured = true
            indicatorX.snapTo(activePos.first)
            indicatorWidth.snapTo(activePos.second)
        } else {
            launch { indicatorX.animateTo(activePos.first, tween(300)) }
            launch { indicatorWidth.animateTo(activePos.second, tween(300)) }
        }
    }

    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(16.dp))
    ) {
        ShineOverlay(
            modifier = Modifier.matchParentSize(),
            opacity = 0.1f
        )
        Box(modifier = Modifier.padding(4.dp).height(IntrinsicSize.Min)) {
            val widthDp = with(density) { indicatorWidth.value.toDp() }
            if (widthDp > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorX.value.toInt(), 0) }
                        .width(widthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }

            Row {
                for ((value, label) in options) {
                    val isActive = value == selectedValue
                    val interaction = remember(value) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .hoverable(interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) { onValueChange(value) }
                            .onGloballyPositioned { cords ->
                                tabPositions[value] = cords.positionInParent().x to cords.size.width.toFloat()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = StudioTypography.medium(14),
                            color = if (isActive || hovered) Color.White else StudioColors.Zinc500
                        )
                    }
                }
            }
        }
    }
}
