package fr.hardel.asset_editor.client.compose.components.mcdoc

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*
import kotlin.jvm.optionals.getOrNull

object McdocDefaults {

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
        is ReferenceType, is DispatcherType, is IndexedType,
        is ConcreteType, is TemplateType, is MappedType -> JsonObject()
    }

    private fun defaultNumber(type: NumericType): Number {
        val range = type.valueRange().getOrNull()
        val minValue = range?.min()?.let { if (it.isPresent) it.asDouble else null }
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
            if (field !is StructPairField) continue
            if (field.optional()) continue
            val key = field.key()
            if (key !is StringKey) continue
            obj.add(key.name(), defaultFor(field.type()))
        }
        return obj
    }

    private fun defaultForUnion(type: UnionType): JsonElement {
        val first = type.members().firstOrNull() ?: return JsonNull.INSTANCE
        return defaultFor(first)
    }
}
