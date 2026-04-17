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
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteItem
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val GIT_BRANCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/git-branch.svg")

@Composable
fun CheckoutBranchDialog(
    visible: Boolean,
    snapshot: GitSnapshot,
    onDismiss: () -> Unit,
    onCheckout: (name: String) -> Unit
) {
    if (!visible) return
    var query by remember { mutableStateOf("") }

    val filtered = remember(snapshot.branches, query) {
        if (query.isBlank()) snapshot.branches
        else snapshot.branches.filter { it.contains(query, ignoreCase = true) }
    }

    val submit = {
        val first = filtered.firstOrNull()
        if (first != null && first != snapshot.currentBranch) onCheckout(first)
        else if (first != null) onDismiss()
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:menu.checkout"),
        value = query,
        onValueChange = { query = it },
        placeholder = I18n.get("changes:dialog.checkout.placeholder"),
        leadingIcon = GIT_BRANCH_ICON,
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        if (snapshot.branches.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:dialog.checkout.none"))
            return@CommandPalette
        }
        if (filtered.isEmpty()) {
            CommandPaletteEmpty(I18n.get("changes:dialog.checkout.empty_filter"))
            return@CommandPalette
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filtered, key = { it }) { branch ->
                val isCurrent = branch == snapshot.currentBranch
                CommandPaletteItem(
                    label = branch,
                    onClick = { onCheckout(branch) },
                    description = if (isCurrent) I18n.get("changes:dialog.checkout.current") else null,
                    enabled = !isCurrent
                )
            }
        }
    }
}
