package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenuItem
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenuSelectTrigger

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
