package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteEmpty
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteItem
import fr.hardel.asset_editor.data.codec.StudioCodecTypeDef
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

data class AddComponentEntry(val id: Identifier, val displayName: String, val description: String?)

@Composable
fun AddComponentModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    definitions: List<StudioCodecTypeDef>,
    excludedIds: Set<Identifier>,
    onPick: (StudioCodecTypeDef) -> Unit
) {
    var query by remember(visible) { mutableStateOf("") }

    val entries = remember(definitions, excludedIds) {
        definitions
            .filter { it.id() !in excludedIds }
            .map { def ->
                val name = StudioTranslation.resolve("component", def.id())
                val descKey = "component:${def.id()}.desc"
                val desc = if (I18n.exists(descKey)) I18n.get(descKey) else null
                AddComponentEntry(def.id(), name, desc) to def
            }
            .sortedBy { it.first.displayName }
    }

    val filtered = remember(entries, query) {
        if (query.isBlank()) entries
        else {
            val needle = query.lowercase()
            entries.filter { (entry, _) ->
                entry.displayName.lowercase().contains(needle) ||
                    entry.id.toString().lowercase().contains(needle)
            }
        }
    }

    CommandPalette(
        visible = visible,
        onDismiss = onDismiss,
        value = query,
        onValueChange = { query = it },
        title = I18n.get("recipe:components.add.title"),
        placeholder = I18n.get("recipe:components.add.search"),
    ) {
        if (filtered.isEmpty()) {
            CommandPaletteEmpty(I18n.get("recipe:components.add.empty"))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
            ) {
                items(
                    items = filtered,
                    key = { (entry, _) -> entry.id.toString() }
                ) { (entry, def) ->
                    CommandPaletteItem(
                        label = entry.displayName,
                        description = entry.description ?: entry.id.toString(),
                        onClick = {
                            onPick(def)
                            onDismiss()
                        },
                        key = entry.id
                    )
                }
            }
        }
    }
}
