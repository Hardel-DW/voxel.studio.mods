package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.AnyHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.BooleanHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.EnumHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.ListHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.LiteralHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.NumericHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.StringHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.StructHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.TupleHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.UnionHead
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*

@Composable
fun Head(
    type: McdocType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    when (type) {
        is NumericType -> NumericHead(type, value, onValueChange, modifier, onClear)
        is BooleanType -> BooleanHead(value, onValueChange, modifier, onClear)
        is StringType -> StringHead(type, value, onValueChange, modifier, onClear)
        is EnumType -> EnumHead(type, value, onValueChange, modifier, onClear)
        is LiteralType -> LiteralHead(type, modifier)
        is StructType -> StructHead(type, value, onValueChange, onClear, modifier)
        is ListType -> ListHead(type, value, onValueChange, modifier)
        is TupleType -> TupleHead(type, value, onValueChange, modifier)
        is UnionType -> UnionHead(type, value, onValueChange, modifier)
        is AnyType, is UnsafeType -> AnyHead(value, onValueChange, modifier)
        else -> AnyHead(value, onValueChange, modifier)
    }
}
