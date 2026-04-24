package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

val FieldRowHeight: Dp = 26.dp
val FieldRowRadius: Dp = 4.dp

/**
 * A label box anchored to the left of a field row. Matches Voxel/Misode visual style —
 * compact, rounded only on the left, shares its right border with the adjacent input.
 */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    minWidth: Dp = 80.dp,
    color: Color = StudioColors.Zinc300
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(FieldRowHeight)
            .widthIn(min = minWidth)
            .clip(RoundedCornerShape(topStart = FieldRowRadius, bottomStart = FieldRowRadius))
            .background(StudioColors.Zinc900)
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(topStart = FieldRowRadius, bottomStart = FieldRowRadius))
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = text,
            style = StudioTypography.medium(12),
            color = color,
            maxLines = 1
        )
    }
}

/**
 * Horizontal compact row that stitches a label and its input together.
 * Children render right of the label with shared borders (visually glued).
 */
@Composable
fun FieldRow(
    label: String,
    modifier: Modifier = Modifier,
    labelMinWidth: Dp = 80.dp,
    labelColor: Color = StudioColors.Zinc300,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = FieldRowHeight)
    ) {
        FieldLabel(text = label, minWidth = labelMinWidth, color = labelColor)
        content()
    }
}
