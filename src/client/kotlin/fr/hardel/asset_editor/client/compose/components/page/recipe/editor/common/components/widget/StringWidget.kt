package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.ComponentTextInput
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n

@Composable
fun StringWidget(
    widget: ComponentWidget.StringWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { value?.asStringOrNull().orEmpty() }
    ComponentTextInput(
        value = current,
        onValueChange = { onValueChange(JsonPrimitive(it)) },
        placeholder = I18n.get("recipe:components.widget.unset"),
        normalize = { it.take(widget.maxLength().orElse(Int.MAX_VALUE)) },
        modifier = modifier
    )
}

private fun JsonElement.asStringOrNull(): String? = runCatching { asString }.getOrNull()
