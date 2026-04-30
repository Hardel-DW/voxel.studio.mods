package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioText
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocSelect
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.SelectOption
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.EnumField
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.EnumType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericEnumValue
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringEnumValue
import net.minecraft.client.resources.language.I18n

@Composable
fun EnumHead(
    type: EnumType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val options = remember(type) {
        type.values().map { field ->
            SelectOption(value = enumFieldText(field), label = StudioText.humanize(field.identifier()))
        }
    }
    val selected = (value as? JsonPrimitive)?.let { p ->
        runCatching { if (p.isString) p.asString else p.asNumber.toString() }.getOrNull()
    }
    McdocSelect(
        options = options,
        selected = selected,
        onSelect = { picked ->
            val field = type.values().find { enumFieldText(it) == picked }
            onValueChange(field?.let(::enumFieldJson) ?: JsonPrimitive(picked))
        },
        modifier = modifier,
        placeholder = I18n.get("mcdoc:widget.unset")
    )
}

private fun enumFieldText(field: EnumField): String = when (val v = field.value()) {
    is StringEnumValue -> v.value()
    is NumericEnumValue -> v.value().toString()
}

private fun enumFieldJson(field: EnumField): JsonElement = when (val v = field.value()) {
    is StringEnumValue -> JsonPrimitive(v.value())
    is NumericEnumValue -> JsonPrimitive(v.value())
}
