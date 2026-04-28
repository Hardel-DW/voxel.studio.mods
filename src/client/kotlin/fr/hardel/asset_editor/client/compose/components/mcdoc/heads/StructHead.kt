package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.defaultFor
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RemoveIconButton
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructType
import net.minecraft.client.resources.language.I18n

@Composable
fun StructHead(
    type: StructType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val present = value is JsonObject

    if (!present) {
        Box(modifier = modifier.fillMaxWidth()) {
            AddFieldButton(
                label = I18n.get("mcdoc:field.add"),
                onClick = { onValueChange(defaultFor(type)) },
                modifier = Modifier.fillMaxWidth(),
                shape = FieldControlShape
            )
        }
        return
    }

    if (onRemove != null) {
        Box(modifier = modifier) {
            RemoveIconButton(onClick = onRemove)
        }
    }
}
