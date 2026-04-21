package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.InputText

@Composable
fun TextOptionRow(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = title,
    maxWidth: Dp = 220.dp
) {
    var text by remember(value) { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (text != value) {
            text = value
        }
    }

    OptionRowLayout(
        title = title,
        description = description,
        modifier = modifier
    ) {
        InputText(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            placeholder = placeholder,
            maxWidth = maxWidth,
            showSearchIcon = false
        )
    }
}
