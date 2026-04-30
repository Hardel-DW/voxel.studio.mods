package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.defaultFor
import fr.hardel.asset_editor.client.compose.components.mcdoc.idPrefix
import fr.hardel.asset_editor.client.compose.components.mcdoc.idRegistry
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryCommandPalette
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RegistryTrigger
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun DynamicKey(
    keyType: McdocType,
    valueType: McdocType,
    obj: JsonObject,
    onObjectChange: (JsonElement) -> Unit
) {
    val registry = idRegistry(keyType.attributes()) ?: return
    val prefix = idPrefix(keyType.attributes()).orEmpty()
    val registryId = remember(registry) { Identifier.tryParse(registry) } ?: return

    var pending by remember { mutableStateOf<Identifier?>(null) }
    var open by remember { mutableStateOf(false) }
    val composedKey = pending?.let { prefix + it.toString() }
    val canAdd = composedKey != null && !obj.has(composedKey)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(McdocTokens.Gap),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            RegistryTrigger(
                label = composedKey,
                placeholder = I18n.get("mcdoc:picker.elements"),
                onClick = { open = !open },
                modifier = Modifier.fillMaxWidth()
            )
            RegistryCommandPalette(
                visible = open,
                registryId = registryId,
                mode = RegistryPickerMode.ELEMENTS,
                selected = pending,
                onPick = { id -> pending = id; open = false },
                onDismiss = { open = false }
            )
        }
        AddFieldButton(
            label = I18n.get("mcdoc:field.add"),
            enabled = canAdd,
            onClick = {
                val key = composedKey ?: return@AddFieldButton
                onObjectChange(obj.deepCopy().apply { add(key, defaultFor(valueType, currentKey = key)) })
                pending = null
            },
            shape = FieldControlShape
        )
    }
}
