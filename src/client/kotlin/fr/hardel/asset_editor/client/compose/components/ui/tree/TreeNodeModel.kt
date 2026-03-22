package fr.hardel.asset_editor.client.compose.components.ui.tree

import net.minecraft.resources.Identifier

class TreeNodeModel {
    val children: LinkedHashMap<String, TreeNodeModel> = LinkedHashMap()
    val identifiers: MutableList<String> = mutableListOf()
    var count: Int = 0
    var elementId: String? = null
    var label: String? = null
    var icon: Identifier? = null
    var isFolder: Boolean = false
}
