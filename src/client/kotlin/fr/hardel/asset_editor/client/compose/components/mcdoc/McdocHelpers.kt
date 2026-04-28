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
import fr.hardel.asset_editor.client.mcdoc.ast.TypeChildren
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import kotlin.jvm.optionals.getOrNull

// ---- Simplification cache + structural predicates ----

@Composable
fun rememberSimplified(type: McdocType, value: JsonElement?): McdocType =
    remember(type, value) {
        McdocService.current().simplifier().simplify(type, value ?: JsonNull.INSTANCE)
    }

fun bindDynamicKey(type: McdocType, key: String): McdocType =
    TypeChildren.walk(type) { t -> if (t is DispatcherType) bindDispatcher(t, key) else t }

private fun bindDispatcher(d: DispatcherType, key: String): DispatcherType {
    var changed = false
    val newIndices = d.parallelIndices().map { idx ->
        val accessor = (idx as? DynamicIndex)?.accessors()?.singleOrNull()
        if (accessor is KeywordAccessor && accessor.keyword() == "key") {
            changed = true
            StaticIndex(key) as Index
        } else idx
    }
    return if (changed) DispatcherType(d.registry(), newIndices, d.attributes()) else d
}

fun hasMcdocHead(type: McdocType): Boolean = type !is StructType

fun hasMcdocBody(type: McdocType, value: JsonElement?): Boolean = when (type) {
    is StructType -> type.fields().isNotEmpty()
    is ListType -> ((value as? JsonArray)?.size() ?: 0) > 0
    is TupleType -> type.items().isNotEmpty()
    is UnionType -> type.members().isNotEmpty()
    else -> false
}

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

// ---- Default JsonElement skeletons ("+ Add" buttons fill the absent value with this) ----

fun defaultFor(type: McdocType): JsonElement = when (type) {
    is BooleanType -> JsonPrimitive(false)
    is NumericType -> JsonPrimitive(defaultNumber(type))
    is StringType -> JsonPrimitive("")
    is LiteralType -> defaultForLiteral(type)
    is EnumType -> defaultForEnum(type)
    is ListType -> JsonArray()
    is TupleType -> defaultForTuple(type)
    is PrimitiveArrayType -> JsonArray()
    is StructType -> defaultForStruct(type)
    is UnionType -> defaultForUnion(type)
    is AnyType, is UnsafeType -> JsonObject()
    is ReferenceType, is DispatcherType, is IndexedType, is ConcreteType, is TemplateType, is MappedType -> JsonNull.INSTANCE
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

// ---- Attribute readers (#[id=...], #[match_regex=...], #[since/until/deprecated=...]) ----

fun idRegistry(attributes: Attributes): String? = readStringAttribute(attributes, "id", "registry")
fun matchRegex(attributes: Attributes): String? = readStringAttribute(attributes, "match_regex", null)
fun deprecatedSince(attributes: Attributes): String? = readStringAttribute(attributes, "deprecated", null)
fun since(attributes: Attributes): String? = readStringAttribute(attributes, "since", null)
fun until(attributes: Attributes): String? = readStringAttribute(attributes, "until", null)

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

private fun readStringAttribute(attributes: Attributes, name: String, treeKey: String?): String? {
    val attr = attributes.get(name).getOrNull() ?: return null
    return when (val value = attr.value().getOrNull()) {
        is Attribute.TypeValue -> readStringFromType(value.type())
        is Attribute.TreeValue -> readTreeString(value, treeKey)
        null -> null
    }
}

private fun readTreeString(tree: Attribute.TreeValue, key: String?): String? {
    val node = if (key == null) tree.positional().firstOrNull() else tree.named()[key]
    return node?.let(::readStringFromValue)
}

private fun readStringFromValue(value: Attribute.AttributeValue): String? = when (value) {
    is Attribute.TypeValue -> readStringFromType(value.type())
    is Attribute.TreeValue -> null
}

private fun readStringFromType(type: McdocType): String? {
    if (type !is LiteralType) return null
    val literal = type.value()
    return if (literal is StringLiteral) literal.value() else null
}

// ---- Java Optional → Kotlin null bridges + NumericRange formatting ----

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
