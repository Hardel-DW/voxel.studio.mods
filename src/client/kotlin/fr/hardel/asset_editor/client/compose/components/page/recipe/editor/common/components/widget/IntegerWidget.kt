package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.ComponentTextInput
import fr.hardel.asset_editor.data.component.ComponentWidget

@Composable
fun IntegerWidget(
    widget: ComponentWidget.IntegerWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { value?.asIntOrNull()?.toString().orEmpty() }
    ComponentTextInput(
        value = current,
        onValueChange = { next ->
            val parsed = next.toIntOrNull() ?: return@ComponentTextInput
            val clamped = parsed
                .coerceAtLeast(widget.min().orElse(Int.MIN_VALUE))
                .coerceAtMost(widget.max().orElse(Int.MAX_VALUE))
            onValueChange(JsonPrimitive(clamped))
        },
        placeholder = placeholder(widget),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

private fun placeholder(widget: ComponentWidget.IntegerWidget): String {
    val min = widget.min().orElse(null)
    val max = widget.max().orElse(null)
    return when {
        min != null && max != null -> "$min – $max"
        min != null -> "≥ $min"
        max != null -> "≤ $max"
        else -> "-- unset --"
    }
}

private fun JsonElement.asIntOrNull(): Int? =
    runCatching { asInt }.getOrNull()
