package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryCommandPalette
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryTrigger
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun TagWidget(
    widget: ComponentWidget.TagWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) {
        runCatching { value?.asString?.let(Identifier::tryParse) }.getOrNull()
    }
    var pickerOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        RegistryTrigger(
            label = current?.let { "#$it" },
            placeholder = I18n.get("recipe:components.widget.unset"),
            onClick = { pickerOpen = !pickerOpen },
            modifier = Modifier.fillMaxWidth()
        )

        RegistryCommandPalette(
            visible = pickerOpen,
            registryId = widget.registry(),
            mode = RegistryPickerMode.TAGS,
            selected = current,
            onPick = { id ->
                onValueChange(JsonPrimitive(id.toString()))
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false }
        )
    }
}
