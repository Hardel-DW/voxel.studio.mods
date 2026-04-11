package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.minecraft.resources.Identifier

@Composable
fun ToolbarLink(
    icon: Identifier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ToolbarButton(icon = icon, onClick = onClick, modifier = modifier, enabled = enabled)
}
