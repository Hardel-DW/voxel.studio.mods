package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.defaultFor
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
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

    if (type.fields().isEmpty()) {
        FlagPresentRow(onRemove, modifier)
        return
    }

    if (onRemove != null) {
        Box(modifier = modifier) {
            RemoveIconButton(onClick = onRemove)
        }
    }
}

@Composable
private fun FlagPresentRow(onRemove: (() -> Unit)?, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(McdocTokens.Gap),
        modifier = modifier
    ) {
        Text(
            text = I18n.get("mcdoc:widget.flag"),
            style = StudioTypography.regular(12),
            color = McdocTokens.TextDimmed
        )
        if (onRemove != null) RemoveIconButton(onClick = onRemove)
    }
}
