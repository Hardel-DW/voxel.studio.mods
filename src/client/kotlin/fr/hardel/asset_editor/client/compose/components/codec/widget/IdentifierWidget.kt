package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecTextInput
import net.minecraft.client.resources.language.I18n

@Composable
fun IdentifierWidget(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { runCatching { value?.asString }.getOrNull().orEmpty() }
    CodecTextInput(
        value = current,
        onValueChange = { onValueChange(JsonPrimitive(it)) },
        placeholder = I18n.get("codec:widget.unset"),
        monospace = true,
        modifier = modifier
    )
}
