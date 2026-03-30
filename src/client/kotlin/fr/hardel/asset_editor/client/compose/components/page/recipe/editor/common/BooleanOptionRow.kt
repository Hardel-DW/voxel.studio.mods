package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.ToggleSwitch

@Composable
fun BooleanOptionRow(
    title: String,
    description: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OptionRowLayout(
        title = title,
        description = description,
        modifier = modifier
    ) {
        ToggleSwitch(
            checked = value,
            onCheckedChange = onValueChange
        )
    }
}
