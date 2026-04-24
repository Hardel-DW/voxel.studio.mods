package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.ComponentTextInput
import fr.hardel.asset_editor.data.codec.CodecWidget

@Composable
fun FloatWidget(
    widget: CodecWidget.FloatWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { value?.asFloatOrNull()?.toPrettyString().orEmpty() }
    ComponentTextInput(
        value = current,
        onValueChange = { next ->
            val parsed = next.toFloatOrNull() ?: return@ComponentTextInput
            val clamped = parsed
                .coerceAtLeast(widget.min().orElse(-Float.MAX_VALUE))
                .coerceAtMost(widget.max().orElse(Float.MAX_VALUE))
            onValueChange(JsonPrimitive(clamped))
        },
        placeholder = placeholder(widget),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

private fun placeholder(widget: CodecWidget.FloatWidget): String {
    val min = widget.min().orElse(null)
    val max = widget.max().orElse(null)
    return when {
        min != null && max != null -> "${min.toPrettyString()} – ${max.toPrettyString()}"
        min != null -> "≥ ${min.toPrettyString()}"
        max != null -> "≤ ${max.toPrettyString()}"
        else -> "0.0"
    }
}

private fun JsonElement.asFloatOrNull(): Float? = runCatching { asFloat }.getOrNull()

private fun Float.toPrettyString(): String {
    if (this == this.toInt().toFloat()) return this.toInt().toString()
    return this.toString()
}
