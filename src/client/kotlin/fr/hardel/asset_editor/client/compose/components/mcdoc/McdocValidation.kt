package fr.hardel.asset_editor.client.compose.components.mcdoc

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ListType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.PrimitiveArrayType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringType
import fr.hardel.asset_editor.client.mcdoc.ast.NumericRange
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.resources.language.I18n

fun fieldError(type: McdocType, value: JsonElement?, optional: Boolean): String? {
    if (isUnset(value)) return if (optional) null else I18n.get("mcdoc:widget.missing_required")
    return contentError(type, value!!)
}

private fun isUnset(value: JsonElement?): Boolean {
    if (value == null || value.isJsonNull) return true
    val prim = value as? JsonPrimitive ?: return false
    return prim.isString && prim.asString.isEmpty()
}

fun contentError(type: McdocType, value: JsonElement): String? = when (type) {
    is NumericType -> rangeError(type.valueRange().getOrNull(), numericValue(value))
    is StringType -> stringError(type, value)
    is ListType -> rangeError(type.lengthRange().getOrNull(), arrayLength(value), lengthMessage = true)
    is PrimitiveArrayType -> rangeError(type.lengthRange().getOrNull(), arrayLength(value), lengthMessage = true)
    else -> null
}

private fun rangeError(range: NumericRange?, candidate: Double?, lengthMessage: Boolean = false): String? {
    if (range == null || candidate == null) return null
    if (range.contains(candidate)) return null
    val key = if (lengthMessage) "mcdoc:error.length" else "mcdoc:error.out_of_range"
    return I18n.get(key).replace("{0}", range.toPlaceholder())
}

private fun stringError(type: StringType, value: JsonElement): String? {
    val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.asString ?: return null
    rangeError(type.lengthRange().getOrNull(), text.length.toDouble(), lengthMessage = true)?.let { return it }
    val regex = matchRegex(type.attributes()) ?: return null
    val matches = runCatching { Regex(regex).matches(text) }.getOrDefault(true)
    return if (matches) null else I18n.get("mcdoc:error.regex")
}

private fun numericValue(value: JsonElement): Double? {
    val prim = value as? JsonPrimitive ?: return null
    if (!prim.isNumber) return null
    return runCatching { prim.asDouble }.getOrNull()
}

private fun arrayLength(value: JsonElement): Double? =
    (value as? JsonArray)?.size()?.toDouble()
