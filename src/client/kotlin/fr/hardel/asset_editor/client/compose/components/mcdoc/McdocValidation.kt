package fr.hardel.asset_editor.client.compose.components.mcdoc

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.mcdoc.ast.Attributes
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.BooleanLiteral
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.BooleanType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.EnumType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ListType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.LiteralType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericEnumValue
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericLiteral
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.PrimitiveArrayType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringEnumValue
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringLiteral
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.TupleType
import fr.hardel.asset_editor.client.mcdoc.ast.NumericRange
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.resources.language.I18n

fun fieldError(type: McdocType, value: JsonElement?, optional: Boolean): String? {
    val absent = value == null || value.isJsonNull
    if (absent) return if (optional) null else I18n.get("mcdoc:widget.missing_required")
    return contentError(type, value)
}

fun contentError(type: McdocType, value: JsonElement): String? = when (type) {
    is NumericType -> numericContentError(type, value)
    is BooleanType -> booleanTypeError(value)
    is StringType -> stringError(type, value)
    is LiteralType -> literalError(type, value)
    is EnumType -> enumError(type, value)
    is ListType -> listLengthError(type.lengthRange().getOrNull(), value)
    is PrimitiveArrayType -> listLengthError(type.lengthRange().getOrNull(), value)
    is TupleType -> tupleArityError(type, value)
    is StructType -> structTypeError(value)
    else -> null
}

private fun numericContentError(type: NumericType, value: JsonElement): String? {
    val prim = value as? JsonPrimitive
    if (prim == null || !prim.isNumber) return typeMismatch("number")
    val num = runCatching { prim.asDouble }.getOrNull() ?: return I18n.get("mcdoc:error.invalid_number")
    return rangeError(type.valueRange().getOrNull(), num)
}

private fun booleanTypeError(value: JsonElement): String? {
    val prim = value as? JsonPrimitive ?: return typeMismatch("boolean")
    return if (prim.isBoolean) null else typeMismatch("boolean")
}

private fun stringError(type: StringType, value: JsonElement): String? {
    val prim = value as? JsonPrimitive ?: return typeMismatch("string")
    if (!prim.isString) return typeMismatch("string")
    val text = prim.asString
    rangeError(type.lengthRange().getOrNull(), text.length.toDouble(), lengthMessage = true)?.let { return it }
    matchRegex(type.attributes())?.let { regex ->
        val matches = runCatching { Regex(regex).matches(text) }.getOrDefault(true)
        if (!matches) return I18n.get("mcdoc:error.regex")
    }
    if (idRegistry(type.attributes()) != null && !idIsDefinition(type.attributes())) {
        resourceLocationError(text, type.attributes())?.let { return it }
    }
    return null
}

private fun literalError(type: LiteralType, value: JsonElement): String? {
    val prim = value as? JsonPrimitive ?: return typeMismatch("literal")
    val expected = type.value()
    val matches = when (expected) {
        is StringLiteral -> prim.isString && prim.asString == expected.value()
        is BooleanLiteral -> prim.isBoolean && prim.asBoolean == expected.value()
        is NumericLiteral -> prim.isNumber && runCatching { prim.asDouble == expected.value() }.getOrDefault(false)
    }
    if (matches) return null
    val display = when (expected) {
        is StringLiteral -> "\"${expected.value()}\""
        is BooleanLiteral -> expected.value().toString()
        is NumericLiteral -> expected.value().toString()
    }
    return I18n.get("mcdoc:error.literal_mismatch").replace("{0}", display)
}

private fun enumError(type: EnumType, value: JsonElement): String? {
    val prim = value as? JsonPrimitive ?: return typeMismatch("enum")
    val matched = type.values().any { v ->
        when (val ev = v.value()) {
            is StringEnumValue -> prim.isString && prim.asString == ev.value()
            is NumericEnumValue -> prim.isNumber && runCatching { prim.asDouble == ev.value() }.getOrDefault(false)
        }
    }
    return if (matched) null else I18n.get("mcdoc:error.enum_mismatch")
}

private fun listLengthError(range: NumericRange?, value: JsonElement): String? {
    val arr = value as? JsonArray ?: return typeMismatch("array")
    return rangeError(range, arr.size().toDouble(), lengthMessage = true)
}

private fun tupleArityError(type: TupleType, value: JsonElement): String? {
    val arr = value as? JsonArray ?: return typeMismatch("array")
    val expected = type.items().size
    if (arr.size() == expected) return null
    return I18n.get("mcdoc:error.tuple_arity").replace("{0}", expected.toString())
}

private fun structTypeError(value: JsonElement): String? {
    if (value is JsonObject) return null
    return typeMismatch("object")
}

private fun rangeError(range: NumericRange?, candidate: Double?, lengthMessage: Boolean = false): String? {
    if (range == null || candidate == null) return null
    if (range.contains(candidate)) return null
    val key = if (lengthMessage) "mcdoc:error.length" else "mcdoc:error.out_of_range"
    return I18n.get(key).replace("{0}", range.toPlaceholder())
}

private fun typeMismatch(expected: String): String =
    I18n.get("mcdoc:error.type_mismatch").replace("{0}", expected)

private val LegalResourceLocationChars: Set<Char> =
    (('a'..'z') + ('0'..'9') + setOf('_', '-', '.')).toSet()

private fun resourceLocationError(text: String, attributes: Attributes): String? {
    if (text.isEmpty()) return I18n.get("mcdoc:error.invalid_id")

    val isTag = text.startsWith("#")
    val tagsMode = tagsMode(attributes)
    if (isTag && tagsMode == TagsMode.NONE) return I18n.get("mcdoc:error.tag_disallowed")
    if (!isTag && tagsMode == TagsMode.REQUIRED) return I18n.get("mcdoc:error.tag_required")

    val body = if (isTag) text.substring(1) else text
    if (body.isEmpty()) return I18n.get("mcdoc:error.invalid_id")

    val sepIdx = body.indexOf(':')
    val namespace = if (sepIdx >= 0) body.substring(0, sepIdx) else null
    val path = if (sepIdx >= 0) body.substring(sepIdx + 1) else body

    val illegal = sortedSetOf<Char>()
    namespace?.forEach { c -> if (c !in LegalResourceLocationChars) illegal.add(c) }
    path.forEach { c -> if (c != '/' && c !in LegalResourceLocationChars) illegal.add(c) }
    if (illegal.isNotEmpty()) {
        return I18n.get("mcdoc:error.invalid_id_chars").replace("{0}", illegal.joinToString(", "))
    }

    if (path.isEmpty()) return I18n.get("mcdoc:error.invalid_id")

    return null
}
