package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.defaultFor
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocSelect
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.SelectOption
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*

@Composable
fun UnionHead(
    type: UnionType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val members = type.members()
    if (members.isEmpty()) return
    val activeIndex = remember(value, members) { selectMember(members, value) }
    val active = members.getOrNull(activeIndex) ?: members.first()
    val activeSimplified = rememberSimplified(active, value)

    val options = remember(members) {
        members.mapIndexed { index, member -> SelectOption(value = index.toString(), label = memberLabel(member, index, members)) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(McdocTokens.Gap),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.widthIn(min = 120.dp, max = 200.dp)) {
            McdocSelect(
                options = options,
                selected = activeIndex.toString(),
                onSelect = { picked ->
                    val newIndex = picked.toIntOrNull() ?: return@McdocSelect
                    if (newIndex != activeIndex) onValueChange(defaultFor(members[newIndex]))
                }
            )
        }
        if (active !is LiteralType && hasMcdocHead(activeSimplified)) {
            Box(modifier = Modifier.weight(1f)) {
                Head(activeSimplified, value, onValueChange)
            }
        }
    }
}

internal fun selectMember(members: List<McdocType>, value: JsonElement?): Int {
    if (value == null || value.isJsonNull) return 0
    val byShape = members.indexOfFirst { matchesShape(it, value) }
    return if (byShape >= 0) byShape else 0
}

private fun matchesShape(type: McdocType, value: JsonElement): Boolean = when (type) {
    is StructType -> value is JsonObject
    is ListType, is TupleType, is PrimitiveArrayType -> value is JsonArray
    is BooleanType -> value is JsonPrimitive && runCatching { value.asBoolean }.isSuccess
    is NumericType -> value is JsonPrimitive && value.isNumber
    is StringType, is EnumType -> value is JsonPrimitive && value.isString
    is LiteralType -> matchesLiteral(type.value(), value)
    is AnyType, is UnsafeType -> true
    else -> false
}

private fun matchesLiteral(literal: LiteralValue, value: JsonElement): Boolean {
    if (value !is JsonPrimitive) return false
    return when (literal) {
        is StringLiteral -> value.isString && value.asString == literal.value()
        is BooleanLiteral -> runCatching { value.asBoolean == literal.value() }.getOrDefault(false)
        is NumericLiteral -> value.isNumber && runCatching { value.asDouble == literal.value() }.getOrDefault(false)
    }
}

private fun memberLabel(member: McdocType, index: Int, all: List<McdocType>): String {
    val raw = baseLabel(member, index)
    val sameKindCount = all.count { it::class == member::class }
    if (sameKindCount <= 1) return raw
    if (member is StructType) {
        val firstLiteralKey = member.fields()
            .filterIsInstance<StructPairField>()
            .firstNotNullOfOrNull { (it.key() as? StringKey)?.name() }
        if (firstLiteralKey != null) return firstLiteralKey
    }
    return "$raw ${index + 1}"
}

private fun baseLabel(member: McdocType, index: Int): String = when (member) {
    is StructType -> "object"
    is ListType -> "list"
    is TupleType -> "tuple"
    is StringType -> "string"
    is BooleanType -> "boolean"
    is NumericType -> when (member.kind()) {
        NumericKind.BYTE, NumericKind.SHORT, NumericKind.INT, NumericKind.LONG -> "integer"
        NumericKind.FLOAT, NumericKind.DOUBLE -> "decimal"
    }
    is EnumType -> "enum"
    is LiteralType -> when (val v = member.value()) {
        is StringLiteral -> "\"${v.value()}\""
        is BooleanLiteral -> v.value().toString()
        is NumericLiteral -> v.value().toString()
    }
    is AnyType, is UnsafeType -> "any"
    is PrimitiveArrayType -> "array"
    else -> "option ${index + 1}"
}
