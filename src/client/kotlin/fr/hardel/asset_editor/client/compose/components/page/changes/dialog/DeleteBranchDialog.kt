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
fun DeleteBranchDialog(
    visible: Boolean,
    snapshot: GitSnapshot,
    onDismiss: () -> Unit,
    onDelete: (name: String) -> Unit
) {
    if (!visible) return
    var query by remember { mutableStateOf("") }

    val deletable = remember(snapshot.branches, snapshot.currentBranch, query) {
        val base = snapshot.branches.filter { it != snapshot.currentBranch }
        if (query.isBlank()) base
        else base.filter { it.contains(query, ignoreCase = true) }
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:menu.branch.delete"),
        value = query,
        onValueChange = { query = it },
        placeholder = I18n.get("changes:dialog.branch.delete.placeholder"),
        onDismiss = onDismiss
    ) {
        if (deletable.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:dialog.branch.delete.empty"))
            return@CommandPalette
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = deletable, key = { it }) { branch ->
                CommandPaletteItem(
                    label = branch,
                    onClick = {},
                    trailing = {
                        Button(
                            onClick = { onDelete(branch) },
                            variant = ButtonVariant.GHOST_BORDER,
                            size = ButtonSize.SM,
                            text = I18n.get("changes:dialog.branch.delete.action")
                        )
                    }
                )
            }
        }
    }
}
