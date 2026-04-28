package fr.hardel.asset_editor.client.compose.components.mcdoc

import fr.hardel.asset_editor.client.mcdoc.ast.Attribute
import fr.hardel.asset_editor.client.mcdoc.ast.Attributes
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import kotlin.jvm.optionals.getOrNull

object McdocAttributes {

    fun idRegistry(attributes: Attributes): String? =
        readStringAttribute(attributes, "id", "registry")

    fun matchRegex(attributes: Attributes): String? =
        readStringAttribute(attributes, "match_regex", null)

    fun deprecatedSince(attributes: Attributes): String? =
        readStringAttribute(attributes, "deprecated", null)

    fun since(attributes: Attributes): String? =
        readStringAttribute(attributes, "since", null)

    fun until(attributes: Attributes): String? =
        readStringAttribute(attributes, "until", null)

    fun isTagged(attributes: Attributes): Boolean {
        val attr = attributes.get("id").getOrNull() ?: return false
        val value = attr.value().getOrNull() ?: return false
        if (value !is Attribute.TreeValue) return false
        val tags = value.named()["tags"] ?: return false
        return readStringFromValue(tags) != null
    }

    private fun readStringAttribute(attributes: Attributes, name: String, treeKey: String?): String? {
        val attr = attributes.get(name).getOrNull() ?: return null
        val value = attr.value().getOrNull() ?: return null
        return when (value) {
            is Attribute.TypeValue -> readStringFromType(value.type())
            is Attribute.TreeValue -> readTreeString(value, treeKey)
        }
    }

    private fun readTreeString(tree: Attribute.TreeValue, key: String?): String? {
        if (key == null) {
            val first = tree.positional().firstOrNull() ?: return null
            return readStringFromValue(first)
        }
        val node = tree.named()[key] ?: return null
        return readStringFromValue(node)
    }

    private fun readStringFromValue(value: Attribute.AttributeValue): String? = when (value) {
        is Attribute.TypeValue -> readStringFromType(value.type())
        is Attribute.TreeValue -> null
    }

    private fun readStringFromType(type: McdocType): String? {
        if (type !is McdocType.LiteralType) return null
        val literal = type.value()
        return if (literal is McdocType.StringLiteral) literal.value() else null
    }
}
