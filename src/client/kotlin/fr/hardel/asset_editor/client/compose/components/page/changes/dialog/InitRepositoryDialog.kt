package fr.hardel.asset_editor.client.compose.components.page.changes.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteHint
import net.minecraft.client.resources.language.I18n

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
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        Column {
            CommandPaletteHint(I18n.get("changes:layout.init_git.remote_hint"))
            CommandPaletteHint(I18n.get("changes:dialog.init.press_enter"))
        }
    }
}
