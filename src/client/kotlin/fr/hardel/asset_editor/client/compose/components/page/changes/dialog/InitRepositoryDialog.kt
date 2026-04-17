package fr.hardel.asset_editor.client.compose.components.page.changes.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteHint
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val FOLDER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")

@Composable
fun InitRepositoryDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (remoteUrl: String?) -> Unit
) {
    if (!visible) return
    var url by remember { mutableStateOf("") }

    val submit = { onSubmit(url.trim().ifBlank { null }) }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:layout.init_git"),
        value = url,
        onValueChange = { url = it },
        placeholder = I18n.get("changes:layout.init_git.remote_placeholder"),
        leadingIcon = FOLDER_ICON,
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CommandPaletteHint(I18n.get("changes:layout.init_git.remote_hint"))
            CommandPaletteHint(I18n.get("changes:dialog.init.press_enter"))
        }
    }
}
