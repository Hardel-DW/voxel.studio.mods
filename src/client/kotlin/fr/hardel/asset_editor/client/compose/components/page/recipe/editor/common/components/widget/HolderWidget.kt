package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryPicker
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryTrigger
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.resources.Identifier

@Composable
fun HolderWidget(
    widget: ComponentWidget.HolderWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) {
        runCatching { value?.asString?.let(Identifier::tryParse) }.getOrNull()
    }
    var pickerOpen by remember { mutableStateOf(false) }

    RegistryTrigger(
        label = current?.toString(),
        placeholder = "Select...",
        onClick = { pickerOpen = true },
        modifier = modifier.fillMaxWidth()
    )

    if (pickerOpen) {
        RegistryPicker(
            registryId = widget.registry(),
            mode = RegistryPickerMode.ELEMENTS,
            selected = current,
            onPick = { id ->
                onValueChange(JsonPrimitive(id.toString()))
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false }
        )
    }
}
