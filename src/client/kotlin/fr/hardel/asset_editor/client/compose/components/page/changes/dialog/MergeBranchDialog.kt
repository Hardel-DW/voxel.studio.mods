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
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteEmpty
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteHint
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteItem
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val GIT_BRANCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/git-branch.svg")

@Composable
fun MergeBranchDialog(
    visible: Boolean,
    snapshot: GitSnapshot,
    onDismiss: () -> Unit,
    onMerge: (name: String) -> Unit
) {
    if (!visible) return
    var query by remember { mutableStateOf("") }
    val current = snapshot.currentBranch

    val mergeable = remember(snapshot.branches, current, query) {
        val base = snapshot.branches.filter { it != current }
        if (query.isBlank()) base
        else base.filter { it.contains(query, ignoreCase = true) }
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:menu.branch.merge"),
        value = query,
        onValueChange = { query = it },
        placeholder = I18n.get("changes:dialog.merge.placeholder"),
        leadingIcon = GIT_BRANCH_ICON,
        onDismiss = onDismiss
    ) {
        if (current == null) {
            CommandPaletteEmpty(I18n.get("changes:dialog.merge.detached"))
            return@CommandPalette
        }
        val base = snapshot.branches.filter { it != current }
        if (base.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:dialog.merge.empty"))
            return@CommandPalette
        }
        CommandPaletteHint(
            I18n.get("changes:dialog.merge.into").replace("{branch}", current)
        )
        if (mergeable.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:dialog.merge.empty_filter"))
            return@CommandPalette
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = mergeable, key = { it }) { branch ->
                CommandPaletteItem(
                    label = branch,
                    onClick = { onMerge(branch) }
                )
            }
        }
    }
}
