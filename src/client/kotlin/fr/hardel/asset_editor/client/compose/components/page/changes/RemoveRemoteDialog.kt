package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteRow
import fr.hardel.asset_editor.client.compose.components.ui.FloatingCommandPalette
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

    FloatingCommandPalette(
        visible = true,
        title = I18n.get("changes:remote.remove.title"),
        searchValue = query,
        onSearchChange = { query = it },
        searchPlaceholder = I18n.get("changes:remote.remove.placeholder"),
        onDismiss = onDismiss
    ) {
        if (snapshot.remotes.isEmpty()) {
            Text(
                text = I18n.get("changes:remote.remove.none"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
            )
            return@FloatingCommandPalette
        }
        if (filtered.isEmpty()) {
            Text(
                text = I18n.get("changes:remote.remove.empty_filter"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
            )
            return@FloatingCommandPalette
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filtered, key = { it.name }) { remote ->
                CommandPaletteRow(
                    label = remote.name,
                    description = remote.url,
                    trailing = {
                        Button(
                            onClick = { onRemove(remote.name) },
                            variant = ButtonVariant.GHOST_BORDER,
                            size = ButtonSize.SM,
                            text = I18n.get("changes:remote.remove.action")
                        )
                    },
                    onClick = {}
                )
            }
        }
    }
}
