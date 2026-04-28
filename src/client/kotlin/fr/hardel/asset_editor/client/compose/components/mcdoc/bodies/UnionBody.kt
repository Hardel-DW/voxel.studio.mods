package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocDefaults
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CodecTokens.Gap),
        modifier = modifier.fillMaxWidth()
    ) {
        UnionTabs(members, activeIndex) { newIndex ->
            if (newIndex != activeIndex) onValueChange(McdocDefaults.defaultFor(members[newIndex]))
        }
        if (hasMcdocHead(activeSimplified)) {
            Box(modifier = Modifier.weight(1f)) {
                McdocHead(activeSimplified, value, onValueChange)
            }
        }
    }
}

@Composable
fun UnionBody(
    type: UnionType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val members = type.members()
    if (members.isEmpty()) return
    val activeIndex = selectMember(members, value)
    val active = members.getOrNull(activeIndex) ?: members.first()
    val activeSimplified = rememberSimplified(active, value)
    if (!hasMcdocBody(activeSimplified, value)) return
    McdocBody(activeSimplified, value, onValueChange, modifier)
}

@Composable
private fun UnionTabs(
    members: List<McdocType>,
    activeIndex: Int,
    onSelect: (Int) -> Unit
) {
    val shape = RoundedCornerShape(CodecTokens.Radius)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(CodecTokens.LabelBg, shape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        members.forEachIndexed { index, member ->
            val label = memberLabel(member, index)
            UnionTab(label = label, selected = index == activeIndex, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun UnionTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(CodecTokens.Radius)
    val bg by animateColorAsState(
        targetValue = when {
            selected -> CodecTokens.Selected
            hovered -> CodecTokens.HoverBg
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "union-tab-bg"
    )
    val fg = if (selected) CodecTokens.Text else CodecTokens.TextDimmed
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(26.dp)
            .clip(shape)
            .background(bg, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Text(label, style = StudioTypography.medium(11), color = fg)
    }
}

private fun memberLabel(member: McdocType, index: Int): String = when (member) {
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

private fun selectMember(members: List<McdocType>, value: JsonElement?): Int {
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
