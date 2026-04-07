package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.Dropdown

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
        Dropdown(
            items = options,
            selected = selected,
            labelExtractor = { it },
            onSelect = onSelect
        )
    }
}
