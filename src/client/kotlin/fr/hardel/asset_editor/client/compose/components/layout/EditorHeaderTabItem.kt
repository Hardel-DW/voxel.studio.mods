package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun EditorHeaderTabItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // Link: px-4 py-2 text-sm font-medium rounded-t-lg transition-all border-b-2 text-zinc-400 border-transparent
    // active -> text-white border-white/60 bg-white/5
    // hover -> hover:text-zinc-200 hover:bg-white/5
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = if (active || hovered) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .drawBehind {
                if (active) {
                    val stroke = 2.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(0f, size.height - stroke / 2f),
                        end = Offset(size.width, size.height - stroke / 2f),
                        strokeWidth = stroke
                    )
                }
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.medium(13),
            color = when {
                active -> Color.White
                hovered -> StudioColors.Zinc300
                else -> StudioColors.Zinc400
            }
        )
    }
}
