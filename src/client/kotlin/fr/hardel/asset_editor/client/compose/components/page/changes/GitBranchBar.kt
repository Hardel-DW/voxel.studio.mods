package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.git.GitRemoteInfo
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import fr.hardel.asset_editor.client.compose.lib.utils.BrowserUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val GITHUB_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/github.svg")
private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val EXTERNAL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/external-link.svg")

@Composable
fun GitBranchBar(
    state: GitState,
    modifier: Modifier = Modifier
) {
    val snapshot = state.snapshot
    var branchMenuOpen by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        snapshot.lastError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable { state.clearError() }
            ) {
                Text(
                    text = error,
                    style = StudioTypography.regular(10),
                    color = Color(0xFFFCA5A5),
                    maxLines = 3
                )
            }
        }

        BranchDropdown(
            snapshot = snapshot,
            expanded = branchMenuOpen,
            onToggle = { branchMenuOpen = !branchMenuOpen },
            onPick = { branch ->
                branchMenuOpen = false
                if (branch != snapshot.currentBranch) state.checkoutBranch(branch)
            }
        )

        val remoteInfo = remember(snapshot.remoteUrl) { GitRemoteInfo.parse(snapshot.remoteUrl) }
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

@Composable
private fun BranchDropdown(
    snapshot: GitSnapshot,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (String) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(8.dp)

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (isHovered) StudioColors.Zinc800.copy(alpha = 0.5f)
                    else StudioColors.Zinc900.copy(alpha = 0.5f)
                )
                .border(1.dp, StudioColors.Zinc800, shape)
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interaction, indication = null) { onToggle() }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            SvgIcon(location = GITHUB_ICON, size = 14.dp, tint = StudioColors.Zinc400)
            Text(
                text = snapshot.currentBranch ?: I18n.get("github:branch.detached"),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc200,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            SvgIcon(location = CHEVRON_ICON, size = 12.dp, tint = StudioColors.Zinc500)
        }

        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(shape)
                    .background(StudioColors.Zinc950)
                    .border(1.dp, StudioColors.Zinc800, shape)
                    .padding(4.dp)
            ) {
                snapshot.branches.forEach { branch ->
                    BranchRow(
                        label = branch,
                        isActive = branch == snapshot.currentBranch,
                        onClick = { onPick(branch) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BranchRow(label: String, isActive: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isActive -> StudioColors.Zinc800.copy(alpha = 0.8f)
                    isHovered -> StudioColors.Zinc900.copy(alpha = 0.6f)
                    else -> Color.Transparent
                }
            )
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.regular(11),
            color = if (isActive) Color.White else StudioColors.Zinc400
        )
    }
}
