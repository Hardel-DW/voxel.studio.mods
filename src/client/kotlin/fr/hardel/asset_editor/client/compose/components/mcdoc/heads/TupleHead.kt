package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.defaultFor
import fr.hardel.asset_editor.client.compose.components.mcdoc.isInlineable
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.TupleType

@Composable
fun TupleHead(
    type: TupleType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = type.items()
    if (!isInlineTuple(items)) return

    val array = remember(value, items.size) { ensureSize(value, items.size, items) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(McdocTokens.Gap),
        modifier = modifier.fillMaxWidth()
    ) {
        items.forEachIndexed { index, item ->
            val simplifiedItem = rememberSimplified(item, array.get(index))
            Head(
                type = simplifiedItem,
                value = array.get(index),
                onValueChange = { newValue -> onValueChange(replaceAt(array, index, newValue)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

internal fun isInlineTuple(items: List<McdocType>): Boolean =
    items.size <= 4 && items.all(::isInlineable)

internal fun ensureSize(value: JsonElement?, size: Int, items: List<McdocType>): JsonArray {
    val array = (value as? JsonArray) ?: JsonArray()
    if (array.size() == size) return array
    val padded = JsonArray()
    for (i in 0 until size) {
        padded.add(if (i < array.size()) array.get(i) else defaultFor(items[i]))
    }
    return padded
}

internal fun replaceAt(array: JsonArray, index: Int, value: JsonElement): JsonArray {
    val next = JsonArray()
    for (i in 0 until array.size()) next.add(if (i == index) value else array.get(i))
    return next
}
