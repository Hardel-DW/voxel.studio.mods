package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.components.mcdoc.defaultFor
import fr.hardel.asset_editor.client.compose.components.mcdoc.orNull
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.FieldControlShape
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ListType
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.resources.language.I18n

@Composable
fun ListHead(
    type: ListType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = (value as? JsonArray)?.size() ?: 0
    val maxSize = type.lengthRange().getOrNull()?.max()?.orNull()?.toInt()
    val canAdd = maxSize == null || size < maxSize

    AddFieldButton(
        label = I18n.get("mcdoc:list.add"),
        enabled = canAdd,
        onClick = { onValueChange(addItem(type, value)) },
        modifier = modifier.fillMaxWidth(),
        shape = FieldControlShape
    )
}

private fun addItem(type: ListType, value: JsonElement?): JsonArray {
    val current = (value as? JsonArray) ?: JsonArray()
    return current.deepCopy().also { it.add(defaultFor(type.item())) }
}
