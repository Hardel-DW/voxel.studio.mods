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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.changes.dialog.AddRemoteDialog
import fr.hardel.asset_editor.client.compose.components.page.changes.dialog.CheckoutBranchDialog
import fr.hardel.asset_editor.client.compose.components.page.changes.dialog.CreateBranchDialog
import fr.hardel.asset_editor.client.compose.components.page.changes.dialog.InitRepositoryDialog
import fr.hardel.asset_editor.client.compose.components.page.changes.dialog.RemoveRemoteDialog
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.ChangesDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import fr.hardel.asset_editor.client.compose.lib.git.rememberGitState
import fr.hardel.asset_editor.client.compose.routes.changes.ChangesPage
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val HEADER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")
private val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")
private val GITHUB_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/github.svg")
private val RELOAD_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/reload.svg")
private val MORE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/more.svg")

private enum class FloatingDialog { NONE, ADD_REMOTE, REMOVE_REMOTE, CREATE_BRANCH, CHECKOUT, INIT }

object ChangesView {
    const val CONCEPT = "concept"
    const val FILE = "file"
}

@Composable
fun ChangesLayout(context: StudioContext, destination: ChangesDestination) {
    val gitState = rememberGitState(context)
    val snapshot = gitState.snapshot

    var commitMessage by rememberSaveable { mutableStateOf("") }
    var viewMode by rememberSaveable { mutableStateOf(ChangesView.CONCEPT) }
    var floatingDialog by remember { mutableStateOf(FloatingDialog.NONE) }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().background(StudioColors.Sidebar)) {
            Column(
                modifier = Modifier
                    .width(288.dp)
                    .fillMaxHeight()
                    .background(StudioColors.Zinc950.copy(alpha = 0.75f))
                    .border(
                        width = 1.dp,
                        color = StudioColors.Zinc800.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(0.dp)
                    )
            ) {
                SidebarHeader(
                    count = snapshot.status.size,
                    loading = snapshot.isLoading,
                    onReload = { gitState.fetchAndRefresh() },
                    menu = {
                        ChangesActionsMenu(
                            snapshot = snapshot,
                            currentView = viewMode,
                            onViewChange = { viewMode = it },
                            onPull = { gitState.pull() },
                            onPush = { gitState.push() },
                            onCommit = {
                                if (commitMessage.isNotBlank() && snapshot.status.isNotEmpty()) {
                                    gitState.commit(commitMessage.trim(), snapshot.status.keys.toList())
                                    commitMessage = ""
                                }
                            },
                            onCheckout = { floatingDialog = FloatingDialog.CHECKOUT },
                            onCreateBranch = { floatingDialog = FloatingDialog.CREATE_BRANCH },
                            onAddRemote = { floatingDialog = FloatingDialog.ADD_REMOTE },
                            onRemoveRemote = { floatingDialog = FloatingDialog.REMOVE_REMOTE },
                            onInit = { floatingDialog = FloatingDialog.INIT },
                            trigger = {
                                HeaderIconButton(icon = MORE_ICON, enabled = true)
                            }
                        )
                    }
                )

                ActionSection(
                    gitState = gitState,
                    snapshot = snapshot,
                    commitMessage = commitMessage,
                    onCommitMessageChange = { commitMessage = it },
                    onClearCommit = { commitMessage = "" },
                    onInitRequest = { floatingDialog = FloatingDialog.INIT }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    when {
                        !snapshot.gitInstalled -> GitNotInstalledNotice()
                        !snapshot.isRepository -> NotARepositoryNotice()
                        snapshot.status.isEmpty() -> EmptyChangesNotice()
                        viewMode == ChangesView.FILE -> ChangesFileTreeView(
                            status = snapshot.status,
                            selectedFile = destination.selectedFile,
                            onSelect = { path ->
                                context.navigationMemory().navigate(ChangesDestination(path))
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        else -> ChangesConceptTreeView(
                            registries = context.registryAccess(),
                            status = snapshot.status,
                            selectedFile = destination.selectedFile,
                            onSelect = { path ->
                                context.navigationMemory().navigate(ChangesDestination(path))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                SidebarFooter(snapshot = snapshot, gitState = gitState)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(StudioColors.Zinc950)
            ) {
                ChangesPage(gitState = gitState, selectedFile = destination.selectedFile)
            }
        }

        AddRemoteDialog(
            visible = floatingDialog == FloatingDialog.ADD_REMOTE,
            snapshot = snapshot,
            onDismiss = { floatingDialog = FloatingDialog.NONE },
            onSubmit = { name, url ->
                gitState.addRemote(name, url)
                floatingDialog = FloatingDialog.NONE
            }
        )

        RemoveRemoteDialog(
            visible = floatingDialog == FloatingDialog.REMOVE_REMOTE,
            snapshot = snapshot,
            onDismiss = { floatingDialog = FloatingDialog.NONE },
            onRemove = { name ->
                gitState.removeRemote(name)
                floatingDialog = FloatingDialog.NONE
            }
        )

        CreateBranchDialog(
            visible = floatingDialog == FloatingDialog.CREATE_BRANCH,
            onDismiss = { floatingDialog = FloatingDialog.NONE },
            onSubmit = { name ->
                gitState.createBranch(name)
                floatingDialog = FloatingDialog.NONE
            }
        )

        CheckoutBranchDialog(
            visible = floatingDialog == FloatingDialog.CHECKOUT,
            snapshot = snapshot,
            onDismiss = { floatingDialog = FloatingDialog.NONE },
            onCheckout = { name ->
                gitState.checkoutBranch(name)
                floatingDialog = FloatingDialog.NONE
            }
        )

        InitRepositoryDialog(
            visible = floatingDialog == FloatingDialog.INIT,
            onDismiss = { floatingDialog = FloatingDialog.NONE },
            onSubmit = { url ->
                gitState.init(url)
                floatingDialog = FloatingDialog.NONE
            }
        )
    }
}

@Composable
private fun SidebarHeader(
    count: Int,
    loading: Boolean,
    onReload: () -> Unit,
    menu: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                SvgIcon(location = HEADER_ICON, size = 18.dp, tint = StudioColors.Zinc100)
                Text(
                    text = I18n.get("changes:layout.title"),
                    style = StudioTypography.bold(16),
                    color = StudioColors.Zinc100
                )
            }
            HeaderIconButton(
                icon = RELOAD_ICON,
                enabled = !loading,
                onClick = onReload
            )
            Spacer(modifier = Modifier.width(2.dp))
            menu()
        }
        Text(
            text = I18n.get("changes:layout.subtitle").replace("{count}", count.toString()),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc500,
            modifier = Modifier.padding(start = 26.dp)
        )
    }
}

@Composable
private fun HeaderIconButton(icon: Identifier, enabled: Boolean, onClick: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .hoverable(interaction, enabled = enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ) else Modifier
            )
            .alpha(if (enabled) 1f else 0.4f)
    ) {
        SvgIcon(
            location = icon,
            size = 14.dp,
            tint = if (isHovered) Color.White else StudioColors.Zinc400
        )
    }
}

@Composable
private fun ActionSection(
    gitState: GitState,
    snapshot: GitSnapshot,
    commitMessage: String,
    onCommitMessageChange: (String) -> Unit,
    onClearCommit: () -> Unit,
    onInitRequest: () -> Unit
) {
    val primary = ChangesPrimaryAction.resolve(snapshot)
    val primaryEnabled = !snapshot.isLoading && when (primary) {
        ChangesPrimaryAction.COMMIT -> commitMessage.isNotBlank()
        ChangesPrimaryAction.NONE -> false
        else -> true
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        snapshot.remoteUrl?.let { url ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                SvgIcon(location = GITHUB_ICON, size = 12.dp, tint = StudioColors.Zinc500)
                Text(
                    text = url,
                    style = StudioTypography.regular(10),
                    color = StudioColors.Zinc500,
                    maxLines = 1
                )
            }
        }
        InputText(
            value = commitMessage,
            onValueChange = onCommitMessageChange,
            placeholder = I18n.get("github:layout.commit.placeholder"),
            showSearchIcon = false
        )
        Button(
            onClick = {
                when (primary) {
                    ChangesPrimaryAction.INIT -> onInitRequest()
                    ChangesPrimaryAction.PULL -> gitState.pull()
                    ChangesPrimaryAction.COMMIT -> {
                        gitState.commit(commitMessage.trim(), snapshot.status.keys.toList())
                        onClearCommit()
                    }

                    ChangesPrimaryAction.PUSH -> gitState.push()
                    ChangesPrimaryAction.PUBLISH -> gitState.push()
                    ChangesPrimaryAction.NONE -> Unit
                }
            },
            variant = ButtonVariant.DEFAULT,
            size = ButtonSize.SM,
            enabled = primaryEnabled,
            text = primary.label(snapshot),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyChangesNotice() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
    ) {
        Box(modifier = Modifier.size(24.dp)) {
            SvgIcon(location = CHECK_ICON, size = 24.dp, tint = StudioColors.Zinc600)
        }
        Text(
            text = I18n.get("changes:empty.no_changes"),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc600
        )
    }
}

@Composable
private fun GitNotInstalledNotice() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp)
    ) {
        Text(
            text = I18n.get("changes:empty.git_not_installed"),
            style = StudioTypography.bold(12),
            color = StudioColors.Zinc300
        )
        Text(
            text = I18n.get("changes:empty.git_not_installed.hint"),
            style = StudioTypography.regular(10),
            color = StudioColors.Zinc600
        )
    }
}

@Composable
private fun NotARepositoryNotice() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp)
    ) {
        Text(
            text = I18n.get("changes:empty.not_a_repo"),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc600
        )
    }
}

@Composable
private fun SidebarFooter(snapshot: GitSnapshot, gitState: GitState) {
    if (!snapshot.gitInstalled || snapshot.root == null || !snapshot.isRepository) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioColors.Zinc950.copy(alpha = 0.9f))
            .border(
                width = 1.dp,
                color = StudioColors.Zinc800.copy(alpha = 0.5f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(12.dp)
    ) {
        GitBranchBar(state = gitState)
    }
}
