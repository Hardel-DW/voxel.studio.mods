package fr.hardel.asset_editor.client.compose.components.page.changes.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteHint
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")

@Composable
fun RenameBranchDialog(
    visible: Boolean,
    currentBranch: String?,
    onDismiss: () -> Unit,
    onSubmit: (oldName: String, newName: String) -> Unit
) {
    if (!visible) return
    val original = currentBranch ?: return
    var name by remember(original) { mutableStateOf(original) }

    val submit = {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && trimmed != original) onSubmit(original, trimmed)
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:menu.branch.rename"),
        value = name,
        onValueChange = { name = it },
        placeholder = I18n.get("changes:dialog.branch.rename.placeholder"),
        leadingIcon = PENCIL_ICON,
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        CommandPaletteHint(
            I18n.get("changes:dialog.branch.rename.hint").replace("{branch}", original)
        )
    }
}
