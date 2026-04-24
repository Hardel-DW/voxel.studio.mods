package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecSelect
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.SelectOption
import fr.hardel.asset_editor.data.codec.CodecWidget

private val triggerShape = RoundedCornerShape(topEnd = CodecTokens.Radius, bottomEnd = CodecTokens.Radius)

@Composable
fun EnumWidget(
    widget: CodecWidget.EnumWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { runCatching { value?.asString }.getOrNull() }
    val options = remember(widget) {
        widget.values().map { v -> SelectOption(value = v, label = humanize(v)) }
    }

    CodecSelect(
        options = options,
        selected = current,
        onSelect = { picked -> onValueChange(JsonPrimitive(picked)) },
        modifier = modifier,
        placeholder = "— unset —",
        shape = triggerShape
    )
}

private fun humanize(value: String): String =
    value.split('_', '/').joinToString(" ") { part ->
        part.replaceFirstChar { ch -> ch.uppercase() }
    }
