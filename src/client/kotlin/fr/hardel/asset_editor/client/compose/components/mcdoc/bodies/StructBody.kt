package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldLabel
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.IndentBox
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RemoveIconButton
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocDefaults
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.isSelfClearable
import fr.hardel.asset_editor.client.compose.components.mcdoc.isStructural
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*
import net.minecraft.client.resources.language.I18n

@Composable
fun StructBody(
    type: StructType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        for (field in type.fields()) {
            if (field !is StructPairField) continue
            val key = field.key()
            if (key !is StringKey) continue
            FieldRow(
                fieldName = key.name(),
                fieldType = field.type(),
                optional = field.optional(),
                obj = obj,
                onObjectChange = onValueChange
            )
        }
    }
}

@Composable
private fun FieldRow(
    fieldName: String,
    fieldType: fr.hardel.asset_editor.client.mcdoc.ast.McdocType,
    optional: Boolean,
    obj: JsonObject,
    onObjectChange: (JsonElement) -> Unit
) {
    val fieldValue = obj.get(fieldName)
    val simplified = rememberSimplified(fieldType, fieldValue)
    val present = fieldValue != null && !fieldValue.isJsonNull
    val label = humanize(fieldName)
    val updateField = { next: JsonElement -> onObjectChange(obj.deepCopy().apply { add(fieldName, next) }) }
    val removeField = { onObjectChange(obj.deepCopy().apply { remove(fieldName) }) }
    val absentComplex = optional && !present && isStructural(simplified)

    val labelColor = when {
        absentComplex -> CodecTokens.TextMuted
        optional -> CodecTokens.TextDimmed
        else -> CodecTokens.Text
    }

    val selfClearable = optional && present && isSelfClearable(simplified)
    val externalRemove = if (optional && present && !selfClearable) removeField else null
    val inlineClear = if (selfClearable) removeField else null

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FieldLabel(text = label, color = labelColor)
            when {
                absentComplex -> AddFieldButton(
                    label = I18n.get("codec:field.add"),
                    onClick = { updateField(McdocDefaults.defaultFor(simplified)) },
                    modifier = Modifier.weight(1f)
                )

                hasMcdocHead(simplified) -> Box(modifier = Modifier.weight(1f)) {
                    McdocHead(simplified, fieldValue, updateField, onClear = inlineClear)
                }

                else -> Spacer(Modifier.weight(1f))
            }
            if (externalRemove != null) RemoveIconButton(onClick = externalRemove)
        }
        if (!absentComplex && hasMcdocBody(simplified, fieldValue)) {
            IndentBox {
                McdocBody(simplified, fieldValue, updateField)
            }
        }
    }
}

private fun humanize(text: String): String =
    text.replace('_', ' ').split(' ').joinToString(" ") {
        if (it.isEmpty()) it else it[0].uppercase() + it.substring(1).lowercase()
    }
