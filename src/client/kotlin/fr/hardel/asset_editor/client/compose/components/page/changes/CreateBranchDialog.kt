package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.layout.padding
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
import fr.hardel.asset_editor.client.compose.components.ui.FloatingCommandPalette
import net.minecraft.client.resources.language.I18n

@Composable
fun CreateBranchDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String) -> Unit
) {
    if (!visible) return
    var name by remember { mutableStateOf("") }

    val submit = {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            onSubmit(trimmed)
        }
    }

    FloatingCommandPalette(
        visible = true,
        title = I18n.get("github:branch.create"),
        searchValue = name,
        onSearchChange = { name = it },
        searchPlaceholder = I18n.get("github:branch.create.placeholder"),
        onDismiss = onDismiss,
        onSubmit = submit
    ) {
        Text(
            text = I18n.get("changes:dialog.press_enter"),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc500,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}
