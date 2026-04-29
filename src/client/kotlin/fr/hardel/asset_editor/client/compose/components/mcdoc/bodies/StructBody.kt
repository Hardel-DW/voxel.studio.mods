package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.idPrefix
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ComputedKey
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringKey
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructPairField
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructType

@Composable
fun StructBody(
    type: StructType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }
    val pairs = remember(type) { type.fields().filterIsInstance<StructPairField>() }
    val staticFields = remember(pairs) { pairs.filter { it.key() is StringKey } }
    val dynamicFields = remember(pairs) { pairs.filter { it.key() is ComputedKey } }
    val staticKeys = remember(staticFields) { staticFields.map { (it.key() as StringKey).name() }.toSet() }
    val extraKeys = remember(obj, staticKeys) { obj.keySet().filter { it !in staticKeys } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(McdocTokens.Gap)
    ) {
        for (field in staticFields) {
            val key = field.key() as StringKey
            StaticField(
                fieldName = key.name(),
                fieldType = field.type(),
                optional = field.optional(),
                deprecated = field.deprecated(),
                doc = field.doc().orElse(null),
                obj = obj,
                onObjectChange = onValueChange
            )
        }

        for (extraKey in extraKeys) {
            val matched = matchDynamicField(extraKey, dynamicFields) ?: continue
            DynamicField(
                key = extraKey,
                valueType = matched.type(),
                obj = obj,
                onObjectChange = onValueChange
            )
        }

        for (field in dynamicFields) {
            val keyType = (field.key() as ComputedKey).type()
            DynamicKey(
                keyType = keyType,
                valueType = field.type(),
                obj = obj,
                onObjectChange = onValueChange
            )
        }
    }
}

private fun matchDynamicField(key: String, defs: List<StructPairField>): StructPairField? {
    val prefixed = defs.firstOrNull { def -> keyPrefix(def)?.let(key::startsWith) == true }
    if (prefixed != null) return prefixed
    return defs.firstOrNull { def -> keyPrefix(def) == null } ?: defs.firstOrNull()
}

private fun keyPrefix(def: StructPairField): String? =
    idPrefix((def.key() as ComputedKey).type().attributes())?.takeIf { it.isNotEmpty() }
