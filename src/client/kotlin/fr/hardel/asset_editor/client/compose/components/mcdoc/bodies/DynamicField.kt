package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.Body
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.Key
import fr.hardel.asset_editor.client.compose.components.mcdoc.bindDynamicKey
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.isCompactInline
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.IndentBox
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RemoveIconButton
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType

private val CompactHeadMaxWidth = 280.dp

@Composable
fun DynamicField(
    key: String,
    valueType: McdocType,
    obj: JsonObject,
    onObjectChange: (JsonElement) -> Unit
) {
    val fieldValue = obj.get(key) ?: return
    val boundType = remember(valueType, key) { bindDynamicKey(valueType, key) }
    val simplified = rememberSimplified(boundType, fieldValue)
    val updateField = { next: JsonElement -> onObjectChange(obj.deepCopy().apply { add(key, next) }) }
    val removeField = { onObjectChange(obj.deepCopy().apply { remove(key) }) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Key(label = key, raw = true)
            HeadSlot(simplified, fieldValue, updateField)
            RemoveIconButton(onClick = removeField)
        }
        if (hasMcdocBody(simplified, fieldValue)) {
            IndentBox {
                Body(simplified, fieldValue, updateField)
            }
        }
    }
}

@Composable
private fun RowScope.HeadSlot(
    simplified: McdocType,
    fieldValue: JsonElement?,
    updateField: (JsonElement) -> Unit
) {
    if (!hasMcdocHead(simplified)) {
        Spacer(Modifier.weight(1f))
        return
    }
    if (isCompactInline(simplified)) {
        Box(modifier = Modifier.widthIn(max = CompactHeadMaxWidth)) {
            Head(simplified, fieldValue, updateField)
        }
        Spacer(Modifier.weight(1f))
        return
    }
    Box(modifier = Modifier.weight(1f)) {
        Head(simplified, fieldValue, updateField)
    }
}
