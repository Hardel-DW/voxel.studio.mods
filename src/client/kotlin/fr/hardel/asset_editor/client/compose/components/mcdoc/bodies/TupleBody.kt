package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.components.mcdoc.Body
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.Key
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.ensureSize
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.isInlineTuple
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.replaceAt
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.TupleType

@Composable
fun TupleBody(
    type: TupleType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = type.items()
    if (isInlineTuple(items)) return

    val array = remember(value, items.size) { ensureSize(value, items.size, items) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(McdocTokens.Gap)
    ) {
        items.forEachIndexed { index, item ->
            val simplifiedItem = rememberSimplified(item, array.get(index))
            val updateItem = { newValue: JsonElement -> onValueChange(replaceAt(array, index, newValue)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Key(label = "[$index]", raw = true, color = McdocTokens.TextDimmed)
                if (hasMcdocHead(simplifiedItem)) {
                    Row(modifier = Modifier.weight(1f)) {
                        Head(simplifiedItem, array.get(index), updateItem)
                    }
                }
            }
            if (hasMcdocBody(simplifiedItem, array.get(index))) {
                Body(simplifiedItem, array.get(index), updateItem)
            }
        }
    }
}
