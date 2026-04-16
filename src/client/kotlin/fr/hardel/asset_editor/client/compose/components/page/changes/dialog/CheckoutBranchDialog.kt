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

    FloatingCommandPalette(
        visible = true,
        title = I18n.get("changes:menu.checkout"),
        searchValue = query,
        onSearchChange = { query = it },
        searchPlaceholder = I18n.get("changes:dialog.checkout.placeholder"),
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        if (snapshot.branches.isEmpty()) {
            Text(
                text = I18n.get("changes:dialog.checkout.none"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
            )
            return@FloatingCommandPalette
        }
        if (filtered.isEmpty()) {
            Text(
                text = I18n.get("changes:dialog.checkout.empty_filter"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
            )
            return@FloatingCommandPalette
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filtered, key = { it }) { branch ->
                val isCurrent = branch == snapshot.currentBranch
                CommandPaletteRow(
                    label = branch,
                    description = if (isCurrent) I18n.get("changes:dialog.checkout.current") else null,
                    enabled = !isCurrent,
                    onClick = { onCheckout(branch) }
                )
            }
        }
    }
}
