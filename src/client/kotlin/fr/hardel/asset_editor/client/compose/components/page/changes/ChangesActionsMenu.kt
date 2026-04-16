package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
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
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val PULL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-down.svg")
private val PUSH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-up.svg")
private val COMMIT_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/git-commit.svg")
private val BRANCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/git-branch.svg")
private val PLUS_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")
private val GLOBE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/globe.svg")
private val TRASH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
private val FOLDER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")
private val EYE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/eye.svg")

@Composable
fun ChangesActionsMenu(
    snapshot: GitSnapshot,
    currentView: String,
    onViewChange: (String) -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onCommit: () -> Unit,
    onCheckout: () -> Unit,
    onCreateBranch: () -> Unit,
    onAddRemote: () -> Unit,
    onRemoveRemote: () -> Unit,
    onInit: () -> Unit,
    trigger: @Composable () -> Unit
) {
    DropdownMenu {
        DropdownMenuTrigger(modifier = Modifier) {
            trigger()
        }
        DropdownMenuContent(minWidth = 220.dp) {
            DropdownMenuLabel(text = I18n.get("changes:menu.section.view"))
            DropdownMenuCheckboxItem(
                checked = currentView == ChangesView.CONCEPT,
                onCheckedChange = { onViewChange(ChangesView.CONCEPT) }
            ) {
                MenuLeading(EYE_ICON)
                MenuLabel(I18n.get("changes:view.concept"))
            }
            DropdownMenuCheckboxItem(
                checked = currentView == ChangesView.FILE,
                onCheckedChange = { onViewChange(ChangesView.FILE) }
            ) {
                MenuLeading(FOLDER_ICON)
                MenuLabel(I18n.get("changes:view.file"))
            }

            DropdownMenuSeparator()
            DropdownMenuLabel(text = I18n.get("changes:menu.section.actions"))
            DropdownMenuItem(
                onClick = onPull,
                enabled = snapshot.isRepository && snapshot.hasUpstream && !snapshot.isLoading,
                leading = { MenuLeading(PULL_ICON) }
            ) {
                MenuLabel(I18n.get("github:primary.pull"))
            }
            DropdownMenuItem(
                onClick = onPush,
                enabled = snapshot.canPush && !snapshot.isLoading,
                leading = { MenuLeading(PUSH_ICON) }
            ) {
                MenuLabel(
                    if (snapshot.needsPublish) I18n.get("github:primary.publish")
                    else I18n.get("github:primary.push")
                )
            }
            DropdownMenuItem(
                onClick = onCommit,
                enabled = snapshot.hasChanges && !snapshot.isLoading,
                leading = { MenuLeading(COMMIT_ICON) }
            ) {
                MenuLabel(I18n.get("github:primary.commit"))
            }

            DropdownMenuSub {
                DropdownMenuSubTrigger(
                    leading = { MenuLeading(BRANCH_ICON) }
                ) {
                    MenuLabel(I18n.get("changes:menu.section.branches"))
                }
                DropdownMenuSubContent(minWidth = 180.dp) {
                    DropdownMenuItem(
                        onClick = onCheckout,
                        enabled = snapshot.isRepository && snapshot.branches.isNotEmpty(),
                        leading = { MenuLeading(BRANCH_ICON) }
                    ) {
                        MenuLabel(I18n.get("changes:menu.checkout"))
                    }
                    DropdownMenuItem(
                        onClick = onCreateBranch,
                        enabled = snapshot.isRepository && !snapshot.isLoading,
                        leading = { MenuLeading(PLUS_ICON) }
                    ) {
                        MenuLabel(I18n.get("changes:menu.create_branch"))
                    }
                }
            }

            DropdownMenuSeparator()
            DropdownMenuLabel(text = I18n.get("changes:menu.section.repository"))
            DropdownMenuItem(
                onClick = onAddRemote,
                enabled = snapshot.isRepository && !snapshot.isLoading,
                leading = { MenuLeading(GLOBE_ICON) }
            ) {
                MenuLabel(I18n.get("changes:menu.add_remote"))
            }
            DropdownMenuItem(
                onClick = onRemoveRemote,
                enabled = snapshot.isRepository && !snapshot.isLoading && snapshot.remotes.isNotEmpty(),
                variant = DropdownItemVariant.DESTRUCTIVE,
                leading = { MenuLeading(TRASH_ICON) }
            ) {
                MenuLabel(I18n.get("changes:menu.remove_remote"))
            }
            if (!snapshot.isRepository) {
                DropdownMenuItem(
                    onClick = onInit,
                    enabled = !snapshot.isLoading && snapshot.root != null,
                    leading = { MenuLeading(FOLDER_ICON) }
                ) {
                    MenuLabel(I18n.get("changes:layout.init_git"))
                }
            }
        }
    }
}

@Composable
private fun MenuLeading(icon: Identifier) {
    SvgIcon(location = icon, size = 14.dp, tint = LocalContentColor.current)
}

@Composable
private fun MenuLabel(text: String) {
    Text(text = text, style = StudioTypography.regular(13))
}
