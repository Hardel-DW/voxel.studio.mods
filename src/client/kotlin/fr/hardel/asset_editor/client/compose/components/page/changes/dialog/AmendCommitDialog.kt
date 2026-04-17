package fr.hardel.asset_editor.client.compose.components.page.changes.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteHint
import net.minecraft.client.resources.language.I18n

@Composable
fun AmendCommitDialog(
    visible: Boolean,
    initialMessage: String,
    onDismiss: () -> Unit,
    onSubmit: (message: String?) -> Unit
) {
    if (!visible) return
    var text by remember(initialMessage) { mutableStateOf(initialMessage) }

    val submit = { onSubmit(text.trim().ifBlank { null }) }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:menu.push.amend"),
        value = text,
        onValueChange = { text = it },
        placeholder = I18n.get("changes:dialog.amend.placeholder"),
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        CommandPaletteHint(I18n.get("changes:dialog.amend.hint"))
    }
}
