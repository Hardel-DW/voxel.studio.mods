package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldLabel
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RemoveIconButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RequiredFieldFrame
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n

@Composable
fun ObjectWidget(
    widget: ComponentWidget.ObjectWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        widget.fields().forEach { field ->
            ObjectField(field = field, obj = obj, onObjectChange = onValueChange)
        }
    }
}

@Composable
private fun ObjectField(
    field: ComponentWidget.Field,
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

    if (field.optional() && !present && child.isComplex()) {
        OptionalComplexFieldRow(
            label = label,
            onAdd = { updateField(defaultJsonFor(child)) }
        )
        return
    }

    when (child) {
        is ComponentWidget.ListWidget -> ComplexListFieldRow(
            label = label,
            optional = field.optional(),
            value = fieldValue,
            widget = child,
            onAddItem = { updateField(addListItem(child, fieldValue)) },
            onRemove = field.optionalRemoveAction(obj, key, onObjectChange),
            onValueChange = updateField
        )

        is ComponentWidget.ObjectWidget,
        is ComponentWidget.MapWidget,
        is ComponentWidget.DispatchedWidget -> ComplexFieldRow(
            label = label,
            optional = field.optional(),
            onRemove = field.optionalRemoveAction(obj, key, onObjectChange)
        ) {
            WidgetEditor(widget = child, value = fieldValue, onValueChange = updateField)
        }

        is ComponentWidget.HolderSetWidget -> InlineFieldRow(
            label = label,
            optional = field.optional(),
            requiredMissing = requiredMissing,
            contentOwnsRequiredState = true
        ) {
            HolderSetWidget(
                widget = child,
                value = fieldValue,
                onValueChange = updateField,
                requiredMissing = requiredMissing
            )
        }

        else -> InlineFieldRow(
            label = label,
            optional = field.optional(),
            requiredMissing = requiredMissing
        ) {
            WidgetEditor(widget = child, value = fieldValue, onValueChange = updateField)
        }
    }
}

@Composable
private fun ComplexFieldRow(
    label: String,
    optional: Boolean,
    onRemove: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FieldLabel(text = label, color = fieldLabelColor(optional))
            Spacer(Modifier.weight(1f))
            if (onRemove != null) {
                RemoveIconButton(onClick = onRemove, modifier = Modifier.width(FieldRowHeight))
            }
        }

        content()
    }
}

@Composable
private fun ComplexListFieldRow(
    label: String,
    optional: Boolean,
    value: JsonElement?,
    widget: ComponentWidget.ListWidget,
    onAddItem: () -> Unit,
    onRemove: (() -> Unit)?,
    onValueChange: (JsonElement) -> Unit
) {
    val size = (value as? JsonArray)?.size() ?: 0
    val canAdd = size < widget.maxSize().orElse(Int.MAX_VALUE)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FieldLabel(text = label, color = fieldLabelColor(optional))
            AddFieldButton(
                label = I18n.get("recipe:components.list.add"),
                enabled = canAdd,
                onClick = onAddItem,
                modifier = Modifier.weight(1f)
            )
            if (onRemove != null) {
                RemoveIconButton(onClick = onRemove, modifier = Modifier.width(FieldRowHeight))
            }
        }

        if (size > 0) {
            ListWidget(
                widget = widget,
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.padding(start = 16.dp),
                showAddButton = false
            )
        }
    }
}

@Composable
private fun InlineFieldRow(
    label: String,
    optional: Boolean,
    requiredMissing: Boolean,
    contentOwnsRequiredState: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FieldLabel(text = label, color = fieldLabelColor(optional, faded = optional))
        if (contentOwnsRequiredState) {
            Box(modifier = Modifier.weight(1f)) { content() }
            return@Row
        }
        RequiredFieldFrame(requiredMissing = requiredMissing, modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun OptionalComplexFieldRow(
    label: String,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FieldLabel(text = label, color = StudioColors.Zinc500)
            AddFieldButton(
                label = I18n.get("recipe:components.field.add"),
                onClick = onAdd,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun ComponentWidget.Field.optionalRemoveAction(
    obj: JsonObject,
    key: String,
    onObjectChange: (JsonElement) -> Unit
): (() -> Unit)? {
    if (!optional()) return null
    return { onObjectChange(obj.withoutField(key)) }
}

private fun JsonObject.withField(key: String, value: JsonElement): JsonObject =
    deepCopy().also { it.add(key, value) }

private fun JsonObject.withoutField(key: String): JsonObject =
    deepCopy().also { it.remove(key) }

private fun ComponentWidget.isComplex(): Boolean =
    this is ComponentWidget.ObjectWidget ||
        this is ComponentWidget.ListWidget ||
        this is ComponentWidget.MapWidget ||
        this is ComponentWidget.DispatchedWidget

private fun ComponentWidget.isRequiredValueMissing(value: JsonElement?): Boolean {
    if (value == null || value.isJsonNull) return true
    if (this !is ComponentWidget.HolderSetWidget) return false
    if (value is JsonArray) return value.size() == 0
    if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return false
    val raw = value.asString
    return raw.isBlank() || raw == "#"
}

private fun fieldLabelColor(optional: Boolean, faded: Boolean = false) =
    when {
        faded -> StudioColors.Zinc500
        optional -> StudioColors.Zinc400
        else -> StudioColors.Zinc200
    }

private fun localizedFieldLabel(key: String): String {
    val translationKey = "recipe:components.field.$key"
    val translated = I18n.get(translationKey)
    return if (translated == translationKey) humanizeField(key) else translated
}

private fun humanizeField(key: String): String =
    key.split('_').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
