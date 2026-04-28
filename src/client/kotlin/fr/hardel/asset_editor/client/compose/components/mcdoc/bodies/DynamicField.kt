package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.components.mcdoc.Body
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.Key
import fr.hardel.asset_editor.client.compose.components.mcdoc.bindDynamicKey
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.IndentBox
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RemoveIconButton
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Key(label = key, raw = true)
            if (hasMcdocHead(simplified)) {
                Box(modifier = Modifier.weight(1f)) {
                    Head(simplified, fieldValue, updateField)
                }
            }
            RemoveIconButton(onClick = removeField)
        }
        if (hasMcdocBody(simplified, fieldValue)) {
            IndentBox {
                Body(simplified, fieldValue, updateField)
            }
        }
    }
}
