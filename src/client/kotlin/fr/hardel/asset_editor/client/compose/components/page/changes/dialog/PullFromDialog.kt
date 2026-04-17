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
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteEmpty
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteHint
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteItem
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n

@Composable
fun PullFromDialog(
    visible: Boolean,
    snapshot: GitSnapshot,
    onDismiss: () -> Unit,
    onPullFrom: (remote: String, branch: String) -> Unit
) {
    if (!visible) return
    var query by remember { mutableStateOf("") }
    val branch = snapshot.currentBranch

    val filtered = remember(snapshot.remotes, query) {
        if (query.isBlank()) snapshot.remotes
        else snapshot.remotes.filter { it.name.contains(query, ignoreCase = true) }
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:menu.pull.pull_from"),
        value = query,
        onValueChange = { query = it },
        placeholder = I18n.get("changes:dialog.pull_from.placeholder"),
        onDismiss = onDismiss
    ) {
        if (branch == null) {
            CommandPaletteEmpty(I18n.get("changes:dialog.pull_from.detached"))
            return@CommandPalette
        }
        if (snapshot.remotes.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:dialog.pull_from.no_remotes"))
            return@CommandPalette
        }
        CommandPaletteHint(
            I18n.get("changes:dialog.pull_from.into").replace("{branch}", branch)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filtered, key = { it.name }) { remote ->
                CommandPaletteItem(
                    label = remote.name,
                    onClick = { onPullFrom(remote.name, branch) },
                    description = remote.url
                )
            }
        }
    }
}
