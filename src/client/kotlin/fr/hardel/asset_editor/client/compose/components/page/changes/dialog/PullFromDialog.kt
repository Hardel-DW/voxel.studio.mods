package fr.hardel.asset_editor.client.compose.components.page.changes.dialog

import androidx.compose.foundation.layout.Arrangement
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
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteRow
import fr.hardel.asset_editor.client.compose.components.ui.FloatingCommandPalette
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

    FloatingCommandPalette(
        visible = true,
        title = I18n.get("changes:menu.pull.pull_from"),
        searchValue = query,
        onSearchChange = { query = it },
        searchPlaceholder = I18n.get("changes:dialog.pull_from.placeholder"),
        onDismiss = onDismiss
    ) {
        if (branch == null) {
            DialogHint("changes:dialog.pull_from.detached")
            return@FloatingCommandPalette
        }
        if (snapshot.remotes.isEmpty()) {
            DialogHint("changes:dialog.pull_from.no_remotes")
            return@FloatingCommandPalette
        }
        Text(
            text = I18n.get("changes:dialog.pull_from.into").replace("{branch}", branch),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc500,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filtered, key = { it.name }) { remote ->
                CommandPaletteRow(
                    label = remote.name,
                    description = remote.url,
                    onClick = { onPullFrom(remote.name, branch) }
                )
            }
        }
    }
}

@Composable
internal fun DialogHint(key: String) {
    Text(
        text = I18n.get(key),
        style = StudioTypography.regular(12),
        color = StudioColors.Zinc500,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
    )
}
