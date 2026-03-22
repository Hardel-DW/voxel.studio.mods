package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import net.minecraft.client.resources.language.I18n

@Composable
fun ContentRow(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    toggle: (@Composable () -> Unit)? = null,
    actionKey: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val actionInteraction = remember { MutableInteractionSource() }
    val actionHovered by actionInteraction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onClick != null) Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onClick() }
                    else Modifier
                )
        ) {
            icon?.invoke()

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                content()
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            toggle?.invoke()

            if (actionKey != null || onAction != null) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(VoxelColors.BorderHover.copy(alpha = 0.5f))
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .widthIn(min = 80.dp)
                        .background(
                            color = if (actionHovered) VoxelColors.BorderHover else VoxelColors.SurfaceRaised,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, VoxelColors.Border, RoundedCornerShape(8.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = actionInteraction,
                            indication = null
                        ) { onAction?.invoke() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = I18n.get(actionKey ?: "generic:configure"),
                        style = VoxelTypography.medium(12),
                        color = if (actionHovered) Color.White else VoxelColors.Zinc400
                    )
                }
            }
        }
    }
}
