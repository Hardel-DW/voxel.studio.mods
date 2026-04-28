package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.StructBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructType

@Composable
fun McdocRoot(
    type: McdocType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val simplified = rememberSimplified(type, value)

    if (simplified is StructType && simplified.fields().isNotEmpty() && value is JsonObject) {
        StructBody(simplified, value, onValueChange, modifier)
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(McdocTokens.Gap)
    ) {
        if (hasMcdocHead(simplified)) Head(simplified, value, onValueChange)
        if (hasMcdocBody(simplified, value)) Body(simplified, value, onValueChange)
    }
}
