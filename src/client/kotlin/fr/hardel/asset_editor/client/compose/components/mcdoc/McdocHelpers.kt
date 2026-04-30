package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.mcdoc.McdocService
import fr.hardel.asset_editor.client.mcdoc.ast.Attribute
import fr.hardel.asset_editor.client.mcdoc.ast.Attributes
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*
import fr.hardel.asset_editor.client.mcdoc.ast.NumericRange
import fr.hardel.asset_editor.client.mcdoc.simplify.Simplifier
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import kotlin.jvm.optionals.getOrNull

@Composable
fun rememberSimplified(type: McdocType, value: JsonElement?, currentKey: String? = null): McdocType =
    remember(type, value, currentKey) {
        McdocService.current().simplifier().simplify(type, value ?: JsonNull.INSTANCE, currentKey)
    }

fun hasMcdocHead(type: McdocType): Boolean = when (type) {
    is StructType -> type.fields().isEmpty()
    else -> true
}

fun hasMcdocBody(type: McdocType, value: JsonElement?): Boolean = when (type) {
    is StructType -> type.fields().isNotEmpty()
    is ListType -> ((value as? JsonArray)?.size() ?: 0) > 0
    is TupleType -> type.items().isNotEmpty()
    is UnionType -> type.members().isNotEmpty()
    else -> false
}

fun isFlagStruct(type: McdocType): Boolean = type is StructType && type.fields().isEmpty()

fun isStructural(type: McdocType): Boolean = when (type) {
    is StructType, is ListType, is TupleType, is UnionType -> true
    else -> false
}

fun isSelfClearable(type: McdocType): Boolean = when (type) {
    is NumericType, is BooleanType, is StringType, is EnumType -> true
    else -> false
}

fun isInlineable(type: McdocType): Boolean = when (type) {
    is NumericType, is BooleanType, is StringType, is EnumType, is LiteralType -> true
    else -> false
}

fun isCompactInline(type: McdocType): Boolean = when (type) {
    is BooleanType, is NumericType, is EnumType, is LiteralType -> true
    is StructType -> type.fields().isEmpty()
    else -> false
}

fun defaultFor(type: McdocType, currentKey: String? = null): JsonElement =
    when (val simplified = McdocService.current().simplifier().simplify(type, JsonNull.INSTANCE, currentKey)) {
        is BooleanType -> JsonPrimitive(false)
        is NumericType -> JsonPrimitive(defaultNumber(simplified))
        is StringType -> JsonPrimitive("")
        is LiteralType -> defaultForLiteral(simplified)
        is EnumType -> defaultForEnum(simplified)
        is ListType -> JsonArray()
        is TupleType -> defaultForTuple(simplified)
        is PrimitiveArrayType -> JsonArray()
        is StructType -> defaultForStruct(simplified)
        is UnionType -> defaultForUnion(simplified)
        is AnyType, is UnsafeType -> JsonObject()
        is ReferenceType, is DispatcherType, is IndexedType, is ConcreteType, is TemplateType -> JsonNull.INSTANCE
    }

private fun defaultNumber(type: NumericType): Number {
    val minValue = type.valueRange().getOrNull()?.min()?.orNull()
    val zero = if (minValue == null || minValue <= 0) 0.0 else minValue
    return if (type.kind().isInteger) zero.toLong() else zero
}

private fun defaultForLiteral(type: LiteralType): JsonElement = when (val v = type.value()) {
    is StringLiteral -> JsonPrimitive(v.value())
    is BooleanLiteral -> JsonPrimitive(v.value())
    is NumericLiteral -> JsonPrimitive(if (v.kind().isInteger) v.value().toLong() else v.value())
}

private fun defaultForEnum(type: EnumType): JsonElement {
    val first = type.values().firstOrNull() ?: return JsonNull.INSTANCE
    return when (val v = first.value()) {
        is StringEnumValue -> JsonPrimitive(v.value())
        is NumericEnumValue -> JsonPrimitive(v.value())
    }
}

private fun defaultForTuple(type: TupleType): JsonArray {
    val array = JsonArray()
    type.items().forEach { array.add(defaultFor(it)) }
    return array
}

private fun defaultForStruct(type: StructType): JsonObject {
    val obj = JsonObject()
    for (field in type.fields()) {
        if (field !is StructPairField || field.optional()) continue
        val key = field.key()
        if (key !is StringKey) continue
        obj.add(key.name(), defaultFor(field.type()))
    }
    return obj
}

private fun defaultForUnion(type: UnionType): JsonElement =
    type.members().firstOrNull()?.let(::defaultFor) ?: JsonNull.INSTANCE

/**
 * Computes the JSON value when switching from [oldType] holding [oldValue] to [newType].
 * Tries to preserve user data via four wrap/unwrap heuristics:
 * - `T → [T]`     : wrap into a single-item array
 * - `[T] → T`     : unwrap when the array has at least one item of compatible type
 * - `T → {k: T}`  : wrap into a struct under the first compatible required field
 * - `{k: T} → T`  : extract from a struct field of compatible type
 *
 * Falls back to [defaultFor] when no preservation is possible. Mirrors the
 * `getChange` behaviour from misode/Spyglass (`McdocHelpers.ts`).
 */
fun getChange(newType: McdocType, oldType: McdocType, oldValue: JsonElement): JsonElement {
    val simplifier = McdocService.current().simplifier()
    val newSimplified = simplifier.simplify(newType, JsonNull.INSTANCE)
    val oldSimplified = simplifier.simplify(oldType, oldValue)

    wrapIntoList(newSimplified, oldSimplified, oldValue, simplifier)?.let { return it }
    unwrapFromList(newSimplified, oldSimplified, oldValue, simplifier)?.let { return it }
    wrapIntoStruct(newSimplified, oldSimplified, oldValue, simplifier)?.let { return it }
    unwrapFromStruct(newSimplified, oldSimplified, oldValue, simplifier)?.let { return it }

    return defaultFor(newType)
}

private fun wrapIntoList(newType: McdocType, oldType: McdocType, oldValue: JsonElement, simplifier: Simplifier): JsonElement? {
    if (newType !is ListType) return null
    val itemSimplified = simplifier.simplify(newType.item(), JsonNull.INSTANCE)
    val candidates = if (itemSimplified is UnionType) itemSimplified.members() else listOf(itemSimplified)
    if (candidates.none { quickEqualTypes(oldType, it) }) return null
    return JsonArray().apply { add(oldValue) }
}

private fun unwrapFromList(newType: McdocType, oldType: McdocType, oldValue: JsonElement, simplifier: Simplifier): JsonElement? {
    if (oldType !is ListType || oldValue !is JsonArray || oldValue.size() == 0) return null
    val itemSimplified = simplifier.simplify(oldType.item(), JsonNull.INSTANCE)
    if (itemSimplified is UnionType) return null
    if (!quickEqualTypes(newType, itemSimplified)) return null
    return oldValue.get(0)
}

private fun wrapIntoStruct(newType: McdocType, oldType: McdocType, oldValue: JsonElement, simplifier: Simplifier): JsonElement? {
    if (newType !is StructType) return null
    for (field in newType.fields()) {
        if (field !is StructPairField || field.optional()) continue
        val key = field.key() as? StringKey ?: continue
        val fieldSimplified = simplifier.simplify(field.type(), JsonNull.INSTANCE)
        if (fieldSimplified is UnionType) continue
        if (!quickEqualTypes(fieldSimplified, oldType)) continue
        return JsonObject().apply { add(key.name(), oldValue) }
    }
    return null
}

private fun unwrapFromStruct(newType: McdocType, oldType: McdocType, oldValue: JsonElement, simplifier: Simplifier): JsonElement? {
    if (oldType !is StructType || oldValue !is JsonObject) return null
    for (field in oldType.fields()) {
        if (field !is StructPairField) continue
        val key = field.key() as? StringKey ?: continue
        val fieldSimplified = simplifier.simplify(field.type(), JsonNull.INSTANCE)
        if (fieldSimplified is UnionType) continue
        if (!quickEqualTypes(fieldSimplified, newType)) continue
        return oldValue.get(key.name()) ?: continue
    }
    return null
}

private fun quickEqualTypes(a: McdocType, b: McdocType): Boolean {
    if (a === b) return true
    if (a::class != b::class) return false
    if (a is LiteralType && b is LiteralType) return literalsEqual(a.value(), b.value())
    if (a is StructType && b is StructType) return firstStructKeyEquals(a, b)
    return true
}

private fun literalsEqual(a: LiteralValue, b: LiteralValue): Boolean = when {
    a is StringLiteral && b is StringLiteral -> a.value() == b.value()
    a is BooleanLiteral && b is BooleanLiteral -> a.value() == b.value()
    a is NumericLiteral && b is NumericLiteral -> a.value() == b.value()
    else -> false
}

private fun firstStructKeyEquals(a: StructType, b: StructType): Boolean {
    val keyA = (a.fields().firstOrNull() as? StructPairField)?.key()
    val keyB = (b.fields().firstOrNull() as? StructPairField)?.key()
    if (keyA == null && keyB == null) return true
    if (keyA == null || keyB == null) return false
    return keyA is StringKey && keyB is StringKey && keyA.name() == keyB.name()
}

fun idRegistry(attributes: Attributes): String? = readShorthandOrTreeKey(attributes, "id", "registry")
fun idPrefix(attributes: Attributes): String? = readTreeKeyOnly(attributes, "id", "prefix")
fun idIsDefinition(attributes: Attributes): Boolean = readTreeKeyBoolean(attributes, "id", "definition") == true
fun matchRegex(attributes: Attributes): String? = readShorthandOrTreeKey(attributes, "match_regex", null)

enum class TagsMode { NONE, ALLOWED, IMPLICIT, REQUIRED }

fun tagsMode(attributes: Attributes): TagsMode {
    val attr = attributes.get("id").getOrNull() ?: return TagsMode.NONE
    val value = attr.value().getOrNull() as? Attribute.TreeValue ?: return TagsMode.NONE
    val tags = value.named()["tags"] ?: return TagsMode.NONE
    return when (readStringFromValue(tags)) {
        "allowed" -> TagsMode.ALLOWED
        "implicit" -> TagsMode.IMPLICIT
        "required" -> TagsMode.REQUIRED
        else -> TagsMode.NONE
    }
}

private fun readShorthandOrTreeKey(attributes: Attributes, name: String, treeKey: String?): String? {
    val attr = attributes.get(name).getOrNull() ?: return null
    return when (val value = attr.value().getOrNull()) {
        is Attribute.TypeValue -> readStringFromType(value.type())
        is Attribute.TreeValue -> readTreeString(value, treeKey)
        null -> null
    }
}

private fun readTreeKeyOnly(attributes: Attributes, name: String, treeKey: String): String? {
    val attr = attributes.get(name).getOrNull() ?: return null
    val tree = attr.value().getOrNull() as? Attribute.TreeValue ?: return null
    return readTreeString(tree, treeKey)
}

private fun readTreeKeyBoolean(attributes: Attributes, name: String, treeKey: String): Boolean? {
    val attr = attributes.get(name).getOrNull() ?: return null
    val tree = attr.value().getOrNull() as? Attribute.TreeValue ?: return null
    val node = tree.named()[treeKey] ?: return null
    return readBooleanFromValue(node)
}

private fun readTreeString(tree: Attribute.TreeValue, key: String?): String? {
    val node = if (key == null) tree.positional().firstOrNull() else tree.named()[key]
    return node?.let(::readStringFromValue)
}

private fun readStringFromValue(value: Attribute.AttributeValue): String? = when (value) {
    is Attribute.TypeValue -> readStringFromType(value.type())
    is Attribute.TreeValue -> null
}

private fun readBooleanFromValue(value: Attribute.AttributeValue): Boolean? = when (value) {
    is Attribute.TypeValue -> readBooleanFromType(value.type())
    is Attribute.TreeValue -> null
}

private fun readStringFromType(type: McdocType): String? {
    if (type !is LiteralType) return null
    val literal = type.value()
    return if (literal is StringLiteral) literal.value() else null
}

private fun readBooleanFromType(type: McdocType): Boolean? {
    if (type !is LiteralType) return null
    val literal = type.value()
    return if (literal is BooleanLiteral) literal.value() else null
}

internal fun OptionalDouble.orNull(): Double? = if (isPresent) asDouble else null
internal fun OptionalInt.orNull(): Int? = if (isPresent) asInt else null
internal fun OptionalLong.orNull(): Long? = if (isPresent) asLong else null

internal fun NumericRange.toPlaceholder(): String {
    val min = min().orNull()
    val max = max().orNull()
    return when {
        min != null && max != null -> "$min – $max"
        min != null -> "≥ $min"
        max != null -> "≤ $max"
        else -> ""
    }
}
