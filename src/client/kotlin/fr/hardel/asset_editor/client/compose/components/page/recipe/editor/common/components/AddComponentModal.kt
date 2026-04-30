package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components

import androidx.compose.foundation.layout.Arrangement
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
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

data class AddComponentEntry(val id: Identifier, val displayName: String, val description: String?)

@Composable
fun AddComponentModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    componentIds: List<Identifier>,
    excludedIds: Set<Identifier>,
    onPick: (Identifier) -> Unit
) {
    var query by remember(visible) { mutableStateOf("") }

    val entries = remember(componentIds, excludedIds) {
        componentIds
            .filter { it !in excludedIds }
            .map { id ->
                val name = StudioTranslation.resolve("component", id)
                val descKey = "component:${id}.desc"
                val desc = if (I18n.exists(descKey)) I18n.get(descKey) else null
                AddComponentEntry(id, name, desc)
            }
            .sortedBy { it.displayName }
    }

    val filtered = remember(entries, query) {
        if (query.isBlank()) entries
        else {
            val needle = query.lowercase()
            entries.filter { entry ->
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
                items(items = filtered, key = { it.id.toString() }) { entry ->
                    CommandPaletteItem(
                        label = entry.displayName,
                        description = entry.description ?: entry.id.toString(),
                        onClick = {
                            onPick(entry.id)
                            onDismiss()
                        },
                        key = entry.id
                    )
                }
            }
        }
    }
}
