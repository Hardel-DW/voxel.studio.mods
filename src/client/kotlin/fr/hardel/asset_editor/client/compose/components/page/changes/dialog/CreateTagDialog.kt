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

private val PLUS_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")

@Composable
fun CreateTagDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String) -> Unit
) {
    if (!visible) return
    var name by remember { mutableStateOf("") }

    val submit = {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) onSubmit(trimmed)
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:menu.tag.create"),
        value = name,
        onValueChange = { name = it },
        placeholder = I18n.get("changes:dialog.tag.create.placeholder"),
        leadingIcon = PLUS_ICON,
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        CommandPaletteHint(I18n.get("changes:dialog.tag.create.hint"))
    }
}
