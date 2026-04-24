package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.StructField
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n

@Composable
fun ObjectBody(
    widget: CodecWidget.ObjectWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        widget.fields().forEach { field ->
            ObjectFieldRow(field = field, obj = obj, onObjectChange = onValueChange)
        }
    }
}

@Composable
private fun ObjectFieldRow(
    field: CodecWidget.Field,
    obj: JsonObject,
    onObjectChange: (JsonElement) -> Unit
) {
    val key = field.key()
    val child = field.widget()
    val fieldValue = obj.get(key)
    val present = fieldValue != null && !fieldValue.isJsonNull
    val requiredMissing = !field.optional() && child.isRequiredValueMissing(fieldValue)
    val label = localizedFieldLabel(key)
    val updateField = { newValue: JsonElement -> onObjectChange(obj.withField(key, newValue)) }
    val removeField = { onObjectChange(obj.withoutField(key)) }

    val absentComplex = field.optional() && !present && child.isComplex()

    StructField(
        label = label,
        widget = child,
        value = fieldValue,
        onValueChange = updateField,
        optional = field.optional(),
        requiredMissing = requiredMissing,
        onAddOptional = if (absentComplex) {
            { updateField(defaultJsonFor(child)) }
        } else null,
        onRemoveOptional = if (field.optional() && present) removeField else null
    )
}

private fun JsonObject.withField(key: String, value: JsonElement): JsonObject =
    deepCopy().also { it.add(key, value) }

private fun JsonObject.withoutField(key: String): JsonObject =
    deepCopy().also { it.remove(key) }

private fun CodecWidget.isComplex(): Boolean =
    this is CodecWidget.ObjectWidget ||
        this is CodecWidget.ListWidget ||
        this is CodecWidget.MapWidget ||
        this is CodecWidget.DispatchedWidget

private fun CodecWidget.isRequiredValueMissing(value: JsonElement?): Boolean {
    if (value == null || value.isJsonNull) return true
    if (this !is CodecWidget.HolderSetWidget) return false
    if (value is JsonArray) return value.size() == 0
    if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return false
    val raw = value.asString
    return raw.isBlank() || raw == "#"
}

private fun localizedFieldLabel(key: String): String {
    val translationKey = "codec:field.$key"
    val translated = I18n.get(translationKey)
    return if (translated == translationKey) humanizeField(key) else translated
}

private fun humanizeField(key: String): String =
    key.split('_').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
