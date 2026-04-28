package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.ListBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.ListHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.StructBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.TupleBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.TupleHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.UnionBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.bodies.UnionHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.AnyHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.BooleanHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.EnumHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.LiteralHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.NumericHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.StringHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.UnsafeHead
import fr.hardel.asset_editor.client.mcdoc.McdocService
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*

@Composable
fun McdocEditor(
    type: McdocType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val simplified = remember(type, value) {
        McdocService.current().simplifier().simplify(type, value ?: JsonNull.INSTANCE)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        if (hasMcdocHead(simplified)) McdocHead(simplified, value, onValueChange)
        if (hasMcdocBody(simplified, value)) McdocBody(simplified, value, onValueChange)
    }
}

@Composable
fun McdocHead(
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
        is AnyType -> AnyHead(value, onValueChange, modifier)
        is UnsafeType -> UnsafeHead(modifier)
        is StructType -> Unit
        is ListType -> ListHead(type, value, onValueChange, modifier)
        is TupleType -> TupleHead(type, value, onValueChange, modifier)
        is UnionType -> UnionHead(type, value, onValueChange, modifier)
        is PrimitiveArrayType -> AnyHead(value, onValueChange, modifier)
        else -> UnsafeHead(modifier)
    }
}

@Composable
fun McdocBody(
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

fun hasMcdocHead(type: McdocType): Boolean = type !is StructType

fun hasMcdocBody(type: McdocType, value: JsonElement?): Boolean = when (type) {
    is StructType -> type.fields().isNotEmpty()
    is ListType -> ((value as? JsonArray)?.size() ?: 0) > 0
    is TupleType -> type.items().isNotEmpty()
    is UnionType -> type.members().isNotEmpty()
    is PrimitiveArrayType -> ((value as? JsonArray)?.size() ?: 0) > 0
    else -> false
}

internal fun isStructural(type: McdocType): Boolean = when (type) {
    is StructType, is ListType, is TupleType, is UnionType, is PrimitiveArrayType -> true
    else -> false
}

internal fun isSelfClearable(type: McdocType): Boolean = when (type) {
    is NumericType, is BooleanType, is StringType, is EnumType -> true
    else -> false
}

fun isInlineable(type: McdocType): Boolean = when (type) {
    is NumericType, is BooleanType, is StringType, is EnumType, is LiteralType -> true
    else -> false
}

@Composable
internal fun rememberSimplified(type: McdocType, value: JsonElement?): McdocType =
    remember(type, value) {
        McdocService.current().simplifier().simplify(type, value ?: JsonNull.INSTANCE)
    }
