package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldLabel
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocDefaults
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
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
    if (items.size > 4 || items.any { !isInlinePrimitive(it) }) return

    val array = remember(value, items.size) { ensureSize(value, items.size, items) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CodecTokens.Gap),
        modifier = modifier.fillMaxWidth()
    ) {
        items.forEachIndexed { index, item ->
            val simplifiedItem = rememberSimplified(item, array.get(index))
            McdocHead(
                simplifiedItem,
                array.get(index),
                { newValue -> onValueChange(replaceAt(array, index, newValue)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TupleBody(
    type: TupleType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = type.items()
    if (items.size <= 4 && items.all { isInlinePrimitive(it) }) return

    val array = remember(value, items.size) { ensureSize(value, items.size, items) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        items.forEachIndexed { index, item ->
            val simplifiedItem = rememberSimplified(item, array.get(index))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel(text = "[$index]", color = CodecTokens.TextDimmed)
                if (hasMcdocHead(simplifiedItem)) {
                    Row(modifier = Modifier.weight(1f)) {
                        McdocHead(simplifiedItem, array.get(index), { newValue -> onValueChange(replaceAt(array, index, newValue)) })
                    }
                }
            }
            if (hasMcdocBody(simplifiedItem, array.get(index))) {
                McdocBody(simplifiedItem, array.get(index), { newValue -> onValueChange(replaceAt(array, index, newValue)) })
            }
        }
    }
}

private fun isInlinePrimitive(type: McdocType): Boolean = when (type) {
    is McdocType.NumericType, is McdocType.BooleanType, is McdocType.LiteralType -> true
    else -> false
}

private fun ensureSize(value: JsonElement?, size: Int, items: List<McdocType>): JsonArray {
    val array = (value as? JsonArray) ?: JsonArray()
    if (array.size() == size) return array
    val padded = JsonArray()
    for (i in 0 until size) {
        padded.add(if (i < array.size()) array.get(i) else McdocDefaults.defaultFor(items[i]))
    }
    return padded
}

private fun replaceAt(array: JsonArray, index: Int, value: JsonElement): JsonArray {
    val next = JsonArray()
    for (i in 0 until array.size()) {
        next.add(if (i == index) value else array.get(i))
    }
    return next
}
