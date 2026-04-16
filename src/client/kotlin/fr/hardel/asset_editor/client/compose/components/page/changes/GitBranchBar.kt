package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuItem
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuTrigger
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.rememberDropdownMenuState
import fr.hardel.asset_editor.client.compose.lib.git.GitRemoteInfo
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import fr.hardel.asset_editor.client.compose.lib.utils.BrowserUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val GITHUB_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/github.svg")
private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val EXTERNAL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/external-link.svg")

private val triggerShape = RoundedCornerShape(8.dp)

@Composable
fun GitBranchBar(
    state: GitState,
    modifier: Modifier = Modifier
) {
    val snapshot = state.snapshot

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        snapshot.lastError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioColors.Red500.copy(alpha = 0.12f))
                    .border(1.dp, StudioColors.Red500.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable { state.clearError() }
            ) {
                Text(
                    text = error,
                    style = StudioTypography.regular(10),
                    color = StudioColors.Red300,
                    maxLines = 3
                )
            }
        }

        val menuState = rememberDropdownMenuState()
        DropdownMenu(state = menuState) {
            DropdownMenuTrigger(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(triggerShape)
                    .background(StudioColors.Zinc900.copy(alpha = 0.5f))
                    .border(1.dp, StudioColors.Zinc800, triggerShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                SvgIcon(location = GITHUB_ICON, size = 14.dp, tint = StudioColors.Zinc400)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = snapshot.currentBranch ?: I18n.get("github:branch.detached"),
                    style = StudioTypography.regular(11),
                    color = StudioColors.Zinc200,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                SvgIcon(location = CHEVRON_ICON, size = 12.dp, tint = StudioColors.Zinc500)
            }

            DropdownMenuContent(matchTriggerWidth = true) {
                snapshot.branches.forEach { branch ->
                    val isActive = branch == snapshot.currentBranch
                    DropdownMenuItem(
                        onClick = {
                            if (!isActive) state.checkoutBranch(branch)
                        }
                    ) {
                        Text(
                            text = branch,
                            style = StudioTypography.regular(11),
                            color = if (isActive) Color.White else StudioColors.Zinc400
                        )
                    }
                }
            }
        }

        val remoteInfo = remember(snapshot.remoteUrl) {
            GitRemoteInfo.parse(snapshot.remoteUrl)
        }
        if (remoteInfo != null) {
            Button(
                onClick = {
                    val head = snapshot.currentBranch ?: return@Button
                    BrowserUtils.openBrowser(remoteInfo.compareUrl("main", head))
                },
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                enabled = !snapshot.isLoading && snapshot.currentBranch != null,
                text = I18n.get("github:action.pr"),
                icon = { SvgIcon(location = EXTERNAL_ICON, size = 12.dp, tint = Color.White) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
