package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.InputText

// TSX: flex-1, min-w-64 (256px). In fit-content collapsed toolbar, takes min-width.
@Composable
fun ToolbarSearch(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    InputText(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        maxWidth = 256.dp,
        modifier = modifier.width(256.dp) // Fixed 256dp to prevent stretching collapsed toolbar
    )
}
