package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecTextInput
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldRowHeight
import fr.hardel.asset_editor.data.codec.CodecWidget

@Composable
fun IntegerWidget(
    widget: CodecWidget.IntegerWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { value?.asIntOrNull()?.toString().orEmpty() }
    val currentInt = value?.asIntOrNull()
    val min = widget.min().orElse(Int.MIN_VALUE)
    val max = widget.max().orElse(Int.MAX_VALUE)

    Row(modifier = modifier) {
        StepButton(
            label = "-",
            enabled = currentInt == null || currentInt > min,
            shape = RoundedCornerShape(0.dp),
            onClick = { onValueChange(JsonPrimitive(((currentInt ?: widget.defaultValue().orElse(0)) - 1).coerceIn(min, max))) }
        )
        CodecTextInput(
            value = current,
            onValueChange = { next ->
                val parsed = next.toIntOrNull() ?: return@CodecTextInput
                onValueChange(JsonPrimitive(parsed.coerceIn(min, max)))
            },
            placeholder = placeholder(widget),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(92.dp),
            shape = RoundedCornerShape(0.dp)
        )
        StepButton(
            label = "+",
            enabled = currentInt == null || currentInt < max,
            shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
            onClick = { onValueChange(JsonPrimitive(((currentInt ?: widget.defaultValue().orElse(0)) + 1).coerceIn(min, max))) }
        )
    }
}

@Composable
private fun StepButton(
    label: String,
    enabled: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> StudioColors.Zinc900.copy(alpha = 0.35f)
            hovered -> StudioColors.Zinc700.copy(alpha = 0.9f)
            else -> StudioColors.Zinc800.copy(alpha = 0.75f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "integer-step-bg"
    )
    val fg = when {
        !enabled -> StudioColors.Zinc700
        hovered -> StudioColors.Zinc50
        else -> StudioColors.Zinc300
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.Companion
            .size(FieldRowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.75f), shape)
            .hoverable(interaction)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Text(label, style = StudioTypography.medium(14), color = fg)
    }
}

private fun placeholder(widget: CodecWidget.IntegerWidget): String {
    val min = widget.min().orElse(null)
    val max = widget.max().orElse(null)
    return when {
        min != null && max != null -> "$min – $max"
        min != null -> "≥ $min"
        max != null -> "≤ $max"
        else -> "0"
    }
}

private fun JsonElement.asIntOrNull(): Int? =
    runCatching { asInt }.getOrNull()
