package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import kotlin.collections.iterator

@Composable
fun AnimatedTabs(
    options: LinkedHashMap<String, String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tabPositions = remember { mutableStateMapOf<String, Pair<Float, Float>>() }

    val activePos = tabPositions[selectedValue]
    val indicatorX by animateDpAsState(
        targetValue = with(density) { (activePos?.first ?: 0f).toDp() },
        animationSpec = tween(300)
    )
    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { (activePos?.second ?: 0f).toDp() },
        animationSpec = tween(300)
    )

    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VoxelColors.Zinc800, RoundedCornerShape(16.dp))
    ) {
        ShineOverlay(
            modifier = Modifier.matchParentSize(),
            opacity = 0.1f
        )
        Box(modifier = Modifier.padding(4.dp)) {
            if (indicatorWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorX.roundToPx(), 0) }
                        .width(indicatorWidth)
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
                            .onGloballyPositioned { coords ->
                                tabPositions[value] = coords.positionInParent().x to coords.size.width.toFloat()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = VoxelTypography.medium(14),
                            color = if (isActive || hovered) Color.White else VoxelColors.Zinc500
                        )
                    }
                }
            }
        }
    }
}
