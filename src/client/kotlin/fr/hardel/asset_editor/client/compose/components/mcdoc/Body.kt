package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.ListBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.StructBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.TupleBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.UnionBody
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*

@Composable
fun Body(
    type: McdocType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    when (type) {
        is StructType -> StructBody(type, value, onValueChange, modifier)
        is ListType -> ListBody(type, value, onValueChange, modifier)
        is TupleType -> TupleBody(type, value, onValueChange, modifier)
        is UnionType -> UnionBody(type, value, onValueChange, modifier)
        else -> Unit
    }
}
