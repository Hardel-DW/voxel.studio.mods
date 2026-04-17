package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.DropdownItemVariant
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuCheckboxItem
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuItem
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuLabel
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuSeparator
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuSub
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuSubContent
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuSubTrigger
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuTrigger
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.resources.Identifier

@Composable
fun ChangesActionsMenu(
    snapshot: GitSnapshot,
    currentView: String,
    commitMessage: String,
    callbacks: ChangesMenuCallbacks,
    trigger: @Composable () -> Unit
) {
    val sections = buildChangesMenu(snapshot, currentView, commitMessage, callbacks)

    DropdownMenu {
        DropdownMenuTrigger(modifier = Modifier) { trigger() }
        DropdownMenuContent(minWidth = 240.dp) {
            sections.forEachIndexed { index, section ->
                if (index > 0) DropdownMenuSeparator()
                if (section.label != null) DropdownMenuLabel(text = section.label)
                section.children.forEach { MenuNode(it) }
            }
        }
    }
}

@Composable
private fun MenuNode(node: GitMenuNode) {
    when (node) {
        is GitMenuAction -> ActionItem(node)
        is GitMenuCheckbox -> CheckboxItem(node)
        is GitMenuSubmenu -> SubmenuItem(node)
    }
}

@Composable
private fun ActionItem(node: GitMenuAction) {
    val icon = node.icon
    val trailing = node.trailing
    DropdownMenuItem(
        onClick = node.onClick,
        enabled = node.enabled,
        variant = if (node.destructive) DropdownItemVariant.DESTRUCTIVE else DropdownItemVariant.DEFAULT,
        leading = if (icon != null) ({ Leading(icon) }) else null,
        trailing = if (trailing != null) ({ Trailing(trailing) }) else null
    ) {
        Label(node.label)
    }
}

@Composable
private fun CheckboxItem(node: GitMenuCheckbox) {
    val icon = node.icon
    DropdownMenuCheckboxItem(
        checked = node.checked,
        onCheckedChange = { node.onCheckedChange() }
    ) {
        if (icon != null) Leading(icon)
        Label(node.label)
    }
}

@Composable
private fun SubmenuItem(node: GitMenuSubmenu) {
    val icon = node.icon
    DropdownMenuSub {
        DropdownMenuSubTrigger(leading = if (icon != null) ({ Leading(icon) }) else null) {
            Label(node.label)
        }
        DropdownMenuSubContent(minWidth = 200.dp) {
            node.children.forEach { MenuNode(it) }
        }
    }
}

@Composable
private fun Leading(icon: Identifier) {
    SvgIcon(location = icon, size = 14.dp, tint = LocalContentColor.current)
}

@Composable
private fun Label(text: String) {
    Text(text = text, style = StudioTypography.regular(13))
}

@Composable
private fun Trailing(text: String) {
    Text(
        text = text,
        style = StudioTypography.regular(11),
        color = StudioColors.Zinc500
    )
}
