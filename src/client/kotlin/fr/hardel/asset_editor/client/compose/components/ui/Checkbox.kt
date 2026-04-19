package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Canvas(
        modifier = modifier
            .size(16.dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        val borderColor = when {
            checked -> StudioColors.Zinc400
            hovered -> StudioColors.Zinc500
            else -> StudioColors.Zinc700
        }
        val fillColor = if (checked) StudioColors.Zinc700 else Color.Transparent
        val cornerPx = 3.dp.toPx()

        drawRoundRect(
            color = fillColor,
            cornerRadius = CornerRadius(cornerPx),
            size = Size(size.width, size.height)
        )
        drawRoundRect(
            color = borderColor,
            cornerRadius = CornerRadius(cornerPx),
            size = Size(size.width, size.height),
            style = Stroke(width = 1.5.dp.toPx())
        )

        if (checked) {
            val path = Path().apply {
                moveTo(size.width * 0.25f, size.height * 0.5f)
                lineTo(size.width * 0.42f, size.height * 0.68f)
                lineTo(size.width * 0.75f, size.height * 0.32f)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
