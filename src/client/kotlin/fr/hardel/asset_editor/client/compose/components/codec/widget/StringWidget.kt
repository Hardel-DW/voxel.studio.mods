package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecTextInput
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n

@Composable
fun StringWidget(
    widget: CodecWidget.StringWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val current = remember(value) { value?.asStringOrNull().orEmpty() }
    CodecTextInput(
        value = current,
        onValueChange = { next ->
            if (next.isEmpty() && onClear != null) {
                onClear()
            } else {
                onValueChange(JsonPrimitive(next))
            }
        },
        placeholder = I18n.get("codec:widget.unset"),
        normalize = { it.take(widget.maxLength().orElse(Int.MAX_VALUE)) },
        modifier = modifier
    )
}

private fun JsonElement.asStringOrNull(): String? = runCatching { asString }.getOrNull()
