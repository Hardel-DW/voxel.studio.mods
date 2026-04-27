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
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecTextInput
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldRowHeight
import fr.hardel.asset_editor.data.codec.CodecWidget

@Composable
fun FloatWidget(
    widget: CodecWidget.FloatWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val current = remember(value) { value?.asFloatOrNull()?.toPrettyString().orEmpty() }
    val currentFloat = value?.asFloatOrNull()
    val min = widget.min().orElse(-Float.MAX_VALUE)
    val max = widget.max().orElse(Float.MAX_VALUE)
    val step = stepFor(widget)

    val emit: (Float) -> Unit = { raw ->
        val clamped = raw.coerceIn(min, max)
        onValueChange(JsonPrimitive(roundToStep(clamped, step)))
    }

    Row(modifier = modifier) {
        StepButton(
            label = "-",
            enabled = currentFloat == null || currentFloat > min,
            shape = RoundedCornerShape(0.dp),
            onClick = { emit((currentFloat ?: widget.defaultValue().orElse(0f)) - step) }
        )
        CodecTextInput(
            value = current,
            onValueChange = { next ->
                if (next.isEmpty()) {
                    onClear?.invoke()
                    return@CodecTextInput
                }
                val parsed = next.toFloatOrNull() ?: return@CodecTextInput
                onValueChange(JsonPrimitive(parsed.coerceIn(min, max)))
            },
            placeholder = placeholder(widget),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(110.dp),
            shape = RoundedCornerShape(0.dp)
        )
        StepButton(
            label = "+",
            enabled = currentFloat == null || currentFloat < max,
            shape = RoundedCornerShape(topEnd = CodecTokens.Radius, bottomEnd = CodecTokens.Radius),
            onClick = { emit((currentFloat ?: widget.defaultValue().orElse(0f)) + step) }
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
            !enabled -> CodecTokens.LabelBg
            hovered -> CodecTokens.HoverBg
            else -> CodecTokens.InputBg
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "float-step-bg"
    )
    val fg = when {
        !enabled -> CodecTokens.TextMuted
        hovered -> CodecTokens.Text
        else -> CodecTokens.TextDimmed
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FieldRowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, CodecTokens.Border, shape)
            .hoverable(interaction)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Text(label, style = StudioTypography.medium(14), color = fg)
    }
}

private fun stepFor(widget: CodecWidget.FloatWidget): Float {
    val min = widget.min().orElse(null)
    val max = widget.max().orElse(null)
    if (min != null && max != null && (max - min) <= 10f) return 0.1f
    return 1.0f
}

private fun roundToStep(value: Float, step: Float): Float {
    val precision = if (step >= 1f) 1f else 100f
    return Math.round(value * precision) / precision
}

private fun placeholder(widget: CodecWidget.FloatWidget): String {
    val min = widget.min().orElse(null)
    val max = widget.max().orElse(null)
    return when {
        min != null && max != null -> "${min.toPrettyString()} – ${max.toPrettyString()}"
        min != null -> "≥ ${min.toPrettyString()}"
        max != null -> "≤ ${max.toPrettyString()}"
        else -> widget.defaultValue().map { it.toPrettyString() }.orElse("0")
    }
}

private fun JsonElement.asFloatOrNull(): Float? = runCatching { asFloat }.getOrNull()

private fun Float.toPrettyString(): String {
    if (this == this.toInt().toFloat()) return this.toInt().toString()
    return this.toString()
}
