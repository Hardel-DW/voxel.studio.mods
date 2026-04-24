package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldRowHeight
import fr.hardel.asset_editor.data.component.ComponentWidget

@Composable
fun BooleanWidget(
    widget: ComponentWidget.BooleanWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { value?.asBooleanOrNull() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier
    ) {
        BoolButton(
            label = "False",
            selected = current == false,
            shape = RoundedCornerShape(0.dp),
            onClick = { onValueChange(JsonPrimitive(false)) },
            modifier = Modifier.weight(1f)
        )
        BoolButton(
            label = "True",
            selected = current == true,
            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
            onClick = { onValueChange(JsonPrimitive(true)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BoolButton(
    label: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> StudioColors.Violet500.copy(alpha = 0.25f)
            hovered -> StudioColors.Zinc800
            else -> StudioColors.Zinc900.copy(alpha = 0.6f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "bool-btn-bg"
    )
    val border = if (selected) StudioColors.Violet500.copy(alpha = 0.55f) else StudioColors.Zinc800
    val fg = if (selected) StudioColors.Zinc50 else StudioColors.Zinc400

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(FieldRowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Text(text = label, style = StudioTypography.medium(12), color = fg)
    }
}

private fun JsonElement.asBooleanOrNull(): Boolean? =
    runCatching { asBoolean }.getOrNull()
