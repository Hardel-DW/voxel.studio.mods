package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuItem
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuSelectTrigger

@Composable
fun DropdownOptionRow(
    title: String,
    description: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OptionRowLayout(
        title = title,
        description = description,
        modifier = modifier
    ) {
        DropdownMenu {
            DropdownMenuSelectTrigger(label = selected)
            DropdownMenuContent {
                options.forEach { option ->
                    DropdownMenuItem(onClick = { onSelect(option) }) {
                        Text(
                            text = option,
                            style = StudioTypography.regular(13)
                        )
                    }
                }
            }
        }
    }
}
