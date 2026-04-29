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
import fr.hardel.asset_editor.client.compose.components.mcdoc.TagsMode
import fr.hardel.asset_editor.client.compose.components.mcdoc.idIsDefinition
import fr.hardel.asset_editor.client.compose.components.mcdoc.idRegistry
import fr.hardel.asset_editor.client.compose.components.mcdoc.tagsMode
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTextInput
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryCommandPalette
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryTrigger
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringType
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

@Composable
fun StringHead(
    type: StringType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val registry = idRegistry(type.attributes())
    if (registry != null && !idIsDefinition(type.attributes()) && isPickableRegistry(registry)) {
        IdentifierHead(registry, tagsMode(type.attributes()), value, onValueChange, modifier)
        return
    }
    val current = remember(value) { (value as? JsonPrimitive)?.asString.orEmpty() }

    McdocTextInput(
        value = current,
        onValueChange = { next ->
            if (next.isEmpty() && onClear != null) onClear()
            else onValueChange(JsonPrimitive(next))
        },
        placeholder = "",
        modifier = modifier
    )
}

@Composable
private fun IdentifierHead(
    registry: String,
    tags: TagsMode,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier
) {
    val raw = remember(value) { (value as? JsonPrimitive)?.asString.orEmpty() }
    val ref = remember(raw, tags) { IdentifierRef.parse(raw, tags) }
    var open by remember { mutableStateOf(false) }
    val registryId = remember(registry) { Identifier.tryParse(registry) }
    val pickerMode = if (ref.isTag) RegistryPickerMode.TAGS else RegistryPickerMode.ELEMENTS

    Box(modifier = modifier.fillMaxWidth()) {
        RegistryTrigger(
            label = ref.display,
            placeholder = I18n.get("mcdoc:widget.unset"),
            onClick = { open = !open },
            modifier = Modifier.fillMaxWidth()
        )
        if (registryId != null) {
            RegistryCommandPalette(
                visible = open,
                registryId = registryId,
                mode = pickerMode,
                selected = ref.identifier,
                onPick = { id ->
                    onValueChange(JsonPrimitive(encodePicked(id, pickerMode, tags)))
                    open = false
                },
                onDismiss = { open = false }
            )
        }
    }
}

private data class IdentifierRef(val identifier: Identifier?, val display: String?, val isTag: Boolean) {
    companion object {
        fun parse(raw: String, tags: TagsMode): IdentifierRef {
            if (raw.isEmpty()) return IdentifierRef(null, null, tags == TagsMode.IMPLICIT || tags == TagsMode.REQUIRED)
            val explicitTag = raw.startsWith("#")
            val isTag = explicitTag || tags == TagsMode.IMPLICIT || tags == TagsMode.REQUIRED
            val body = if (explicitTag) raw.substring(1) else raw
            val identifier = runCatching { Identifier.tryParse(body) }.getOrNull()
            val display = if (isTag && !explicitTag) "#$raw" else raw
            return IdentifierRef(identifier, display, isTag)
        }
    }
}

private fun encodePicked(id: Identifier, mode: RegistryPickerMode, tags: TagsMode): String = when {
    mode == RegistryPickerMode.TAGS && tags != TagsMode.IMPLICIT -> "#$id"
    else -> id.toString()
}

private fun isPickableRegistry(registry: String): Boolean {
    val id = Identifier.tryParse(registry) ?: return false
    val key = ResourceKey.createRegistryKey<Any>(id)
    val registries = Minecraft.getInstance().connection?.registryAccess() ?: return false
    return registries.lookup(key).isPresent
}
