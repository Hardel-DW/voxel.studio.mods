package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.Body
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.Key
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.isSelfClearable
import fr.hardel.asset_editor.client.compose.components.mcdoc.isStructural
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.IndentBox
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RemoveIconButton
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType

@Composable
fun StaticField(
    fieldName: String,
    fieldType: McdocType,
    optional: Boolean,
    deprecated: Boolean,
    doc: String?,
    obj: JsonObject,
    onObjectChange: (JsonElement) -> Unit
) {
    val fieldValue = obj.get(fieldName)
    val simplified = rememberSimplified(fieldType, fieldValue)
    val present = fieldValue != null && !fieldValue.isJsonNull
    val updateField = { next: JsonElement -> onObjectChange(obj.deepCopy().apply { add(fieldName, next) }) }
    val removeField = { onObjectChange(obj.deepCopy().apply { remove(fieldName) }) }
    val absentComplex = optional && !present && isStructural(simplified)

    val labelColor = when {
        absentComplex -> McdocTokens.TextMuted
        optional -> McdocTokens.TextDimmed
        else -> McdocTokens.Text
    }

    val selfClearable = optional && present && isSelfClearable(simplified)
    val externalRemove = if (optional && present && !selfClearable && simplified !is McdocType.StructType) removeField else null
    val inlineClear = when {
        selfClearable -> removeField
        optional && present && simplified is McdocType.StructType -> removeField
        else -> null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Key(label = fieldName, doc = doc, deprecated = deprecated, color = labelColor)
            when {
                hasMcdocHead(simplified) -> Box(modifier = Modifier.weight(1f)) {
                    Head(simplified, fieldValue, updateField, onClear = inlineClear)
                }

                simplified is McdocType.StructType -> Box(modifier = Modifier.weight(1f)) {
                    Head(simplified, fieldValue, updateField, onClear = inlineClear)
                }

                else -> Spacer(Modifier.weight(1f))
            }
            if (externalRemove != null) RemoveIconButton(onClick = externalRemove)
        }
        if (!absentComplex && hasMcdocBody(simplified, fieldValue)) {
            IndentBox {
                Body(simplified, fieldValue, updateField)
            }
        }
    }
}
