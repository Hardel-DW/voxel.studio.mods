package fr.hardel.asset_editor.client.compose.components.page.changes.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteEmpty
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteItem
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n

@Composable
fun RemoveRemoteDialog(
    visible: Boolean,
    snapshot: GitSnapshot,
    onDismiss: () -> Unit,
    onRemove: (name: String) -> Unit
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }
    val filtered = remember(snapshot.remotes, query) {
        if (query.isBlank()) snapshot.remotes
        else snapshot.remotes.filter {
            it.name.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:remote.remove.title"),
        value = query,
        onValueChange = { query = it },
        placeholder = I18n.get("changes:remote.remove.placeholder"),
        onDismiss = onDismiss
    ) {
        if (snapshot.remotes.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:remote.remove.none"))
            return@CommandPalette
        }
        if (filtered.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:remote.remove.empty_filter"))
            return@CommandPalette
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filtered, key = { it.name }) { remote ->
                CommandPaletteItem(
                    label = remote.name,
                    onClick = {},
                    description = remote.url,
                    trailing = {
                        Button(
                            onClick = { onRemove(remote.name) },
                            variant = ButtonVariant.GHOST_BORDER,
                            size = ButtonSize.SM,
                            text = I18n.get("changes:remote.remove.action")
                        )
                    }
                )
            }
        }
    }
}
