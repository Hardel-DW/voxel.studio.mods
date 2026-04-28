package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

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
import fr.hardel.asset_editor.client.compose.components.mcdoc.idRegistry
import fr.hardel.asset_editor.client.compose.components.mcdoc.matchRegex
import fr.hardel.asset_editor.client.compose.components.mcdoc.toPlaceholder
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTextInput
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryCommandPalette
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryTrigger
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringType
import fr.hardel.asset_editor.client.mcdoc.ast.NumericRange
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun StringHead(
    type: StringType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val registry = idRegistry(type.attributes())
    if (registry != null) {
        IdentifierHead(registry, value, onValueChange, modifier)
        return
    }
    val current = remember(value) { (value as? JsonPrimitive)?.asString.orEmpty() }
    val regex = matchRegex(type.attributes())
    val lengthRange = type.lengthRange().getOrNull()
    val error = remember(current, regex, lengthRange) { stringError(current, regex, lengthRange) }

    McdocTextInput(
        value = current,
        onValueChange = { next ->
            if (next.isEmpty() && onClear != null) onClear()
            else onValueChange(JsonPrimitive(next))
        },
        placeholder = "",
        modifier = modifier,
        error = error
    )
}

@Composable
private fun IdentifierHead(
    registry: String,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier
) {
    val current = remember(value) {
        runCatching { (value as? JsonPrimitive)?.asString?.let(Identifier::tryParse) }.getOrNull()
    }
    var open by remember { mutableStateOf(false) }
    val registryId = remember(registry) { Identifier.tryParse(registry) }

    Box(modifier = modifier.fillMaxWidth()) {
        RegistryTrigger(
            label = current?.toString(),
            placeholder = I18n.get("mcdoc:widget.unset"),
            onClick = { open = !open },
            modifier = Modifier.fillMaxWidth()
        )
        if (registryId != null) {
            RegistryCommandPalette(
                visible = open,
                registryId = registryId,
                mode = RegistryPickerMode.ELEMENTS,
                selected = current,
                onPick = { id ->
                    onValueChange(JsonPrimitive(id.toString()))
                    open = false
                },
                onDismiss = { open = false }
            )
        }
    }
}

private fun stringError(text: String, regex: String?, lengthRange: NumericRange?): String? {
    if (lengthRange != null && !lengthRange.contains(text.length.toDouble())) {
        return I18n.get("mcdoc:error.length").replace("{0}", lengthRange.toPlaceholder())
    }
    if (regex != null) {
        val matches = runCatching { Regex(regex).matches(text) }.getOrDefault(true)
        if (!matches) return I18n.get("mcdoc:error.regex")
    }
    return null
}
