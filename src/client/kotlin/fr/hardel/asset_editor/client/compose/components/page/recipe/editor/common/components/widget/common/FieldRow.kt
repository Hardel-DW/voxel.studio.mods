package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

val FieldRowHeight: Dp = 32.dp
val FieldRowRadius: Dp = 6.dp
private val WARNING = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/warning.svg")

/**
 * A label box anchored to the left of a field row. Matches Voxel/Misode visual style —
 * compact, rounded only on the left, shares its right border with the adjacent input.
 */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    minWidth: Dp = 120.dp,
    color: Color = StudioColors.Zinc300
) {
    val shape = RoundedCornerShape(topStart = FieldRowRadius, bottomStart = FieldRowRadius)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(FieldRowHeight)
            .widthIn(min = minWidth)
            .clip(shape)
            .background(StudioColors.Zinc800.copy(alpha = 0.52f), shape)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.58f), shape)
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = text,
            style = StudioTypography.medium(12),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    labelMinWidth: Dp = 120.dp,
    labelColor: Color = StudioColors.Zinc300,
    requiredMissing: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = FieldRowHeight)
    ) {
        FieldLabel(text = label, minWidth = labelMinWidth, color = labelColor)
        RequiredFieldFrame(requiredMissing = requiredMissing) { content() }
    }
}

@Composable
fun RequiredFieldFrame(
    requiredMissing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!requiredMissing) {
        Box(modifier = modifier) { content() }
        return
    }

    val shape = RoundedCornerShape(topEnd = FieldRowRadius, bottomEnd = FieldRowRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioColors.Red500.copy(alpha = 0.76f), shape)
    ) {
        content()
        RequiredWarningIcon(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
    }
}

@Composable
private fun RequiredWarningIcon(modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(StudioColors.Red500.copy(alpha = 0.14f), CircleShape)
            .border(1.dp, StudioColors.Red500.copy(alpha = 0.64f), CircleShape)
            .hoverable(interaction)
    ) {
        SvgIcon(WARNING, 14.dp, tint = StudioColors.Red400)
    }

    if (hovered) {
        Popup(alignment = Alignment.TopEnd, offset = IntOffset(0, -34)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioColors.Zinc950, RoundedCornerShape(6.dp))
                    .border(1.dp, StudioColors.Zinc700, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = I18n.get("recipe:components.required"),
                    style = StudioTypography.regular(12),
                    color = StudioColors.Zinc100
                )
            }
        }
    }
}
