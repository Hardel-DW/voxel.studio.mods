package fr.hardel.asset_editor.client.compose.components.page.changes

import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private fun icon(path: String): Identifier =
    Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/$path")

private val ARROW_DOWN = icon("arrow-down.svg")
private val ARROW_UP = icon("arrow-up.svg")
private val COMMIT = icon("git-commit.svg")
private val BRANCH = icon("git-branch.svg")
private val TAG = icon("star.svg")
private val GLOBE = icon("globe.svg")
private val PLUS = icon("plus.svg")
private val TRASH = icon("trash.svg")
private val PENCIL = icon("pencil.svg")
private val RELOAD = icon("reload.svg")
private val EYE = icon("eye.svg")
private val FOLDER = icon("folder.svg")

sealed interface GitMenuNode

data class GitMenuAction(
    val label: String,
    val icon: Identifier?,
    val enabled: Boolean = true,
    val trailing: String? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit
) : GitMenuNode

data class GitMenuCheckbox(
    val label: String,
    val icon: Identifier?,
    val checked: Boolean,
    val onCheckedChange: () -> Unit
) : GitMenuNode

data class GitMenuSubmenu(
    val label: String,
    val icon: Identifier?,
    val children: List<GitMenuNode>
) : GitMenuNode

data class GitMenuSection(
    val label: String? = null,
    val children: List<GitMenuNode>
)

data class ChangesMenuCallbacks(
    val onViewChange: (String) -> Unit,
    val onPull: () -> Unit,
    val onPullFrom: () -> Unit,
    val onPush: () -> Unit,
    val onFetch: () -> Unit,
    val onAmend: () -> Unit,
    val onCommit: () -> Unit,
    val onCheckout: () -> Unit,
    val onMergeBranch: () -> Unit,
    val onRebaseBranch: () -> Unit,
    val onCreateBranch: () -> Unit,
    val onRenameBranch: () -> Unit,
    val onDeleteBranch: () -> Unit,
    val onCreateTag: () -> Unit,
    val onDeleteTag: () -> Unit,
    val onAddRemote: () -> Unit,
    val onRemoveRemote: () -> Unit,
    val onInit: () -> Unit
)

/**
 * Builds the git actions menu as a pure data tree from the snapshot. The renderer is stateless
 * and does not decide enablement — all gating lives here so policy is reviewable in one place.
 */
fun buildChangesMenu(
    snapshot: GitSnapshot,
    currentView: String,
    commitMessage: String,
    callbacks: ChangesMenuCallbacks
): List<GitMenuSection> {
    val repo = snapshot.isRepository
    val loading = snapshot.isLoading
    val hasUpstream = snapshot.hasUpstream
    val hasRemote = snapshot.remotes.isNotEmpty()
    val hasBranches = snapshot.branches.isNotEmpty()
    val hasTags = snapshot.tags.isNotEmpty()
    val hasChanges = snapshot.hasChanges
    val hasMessage = commitMessage.isNotBlank()
    val branchCount = snapshot.branches.size

    val sections = mutableListOf<GitMenuSection>()

    sections += GitMenuSection(
        children = listOf(
            GitMenuSubmenu(
                label = i18n("changes:menu.view.title"),
                icon = EYE,
                children = listOf(
                    GitMenuCheckbox(
                        label = i18n("changes:view.concept"),
                        icon = EYE,
                        checked = currentView == ChangesView.CONCEPT,
                        onCheckedChange = { callbacks.onViewChange(ChangesView.CONCEPT) }
                    ),
                    GitMenuCheckbox(
                        label = i18n("changes:view.file"),
                        icon = FOLDER,
                        checked = currentView == ChangesView.FILE,
                        onCheckedChange = { callbacks.onViewChange(ChangesView.FILE) }
                    )
                )
            )
        )
    )

    if (repo) sections += quickSyncSection(snapshot, hasUpstream, loading, hasChanges, hasMessage, callbacks)
    if (repo) sections += advancedSection(snapshot, hasUpstream, loading, hasRemote, hasChanges, hasMessage, hasBranches, hasTags, branchCount, callbacks)
    sections += remoteSection(repo, loading, hasRemote, snapshot.root != null, callbacks)

    return sections
}

private fun quickSyncSection(
    snapshot: GitSnapshot,
    hasUpstream: Boolean,
    loading: Boolean,
    hasChanges: Boolean,
    hasMessage: Boolean,
    cb: ChangesMenuCallbacks
): GitMenuSection {
    val behind = snapshot.behindCount
    val ahead = snapshot.aheadCount
    val pushLabel = if (snapshot.needsPublish) i18n("changes:menu.push.publish") else i18n("changes:menu.push.push")
    val pushTrailing = if (!snapshot.needsPublish && ahead > 0) "↑$ahead" else null

    return GitMenuSection(
        label = i18n("changes:menu.section.sync"),
        children = listOf(
            GitMenuAction(
                label = i18n("changes:menu.pull.pull"),
                icon = ARROW_DOWN,
                enabled = hasUpstream && !loading,
                trailing = if (behind > 0) "↓$behind" else null,
                onClick = cb.onPull
            ),
            GitMenuAction(
                label = pushLabel,
                icon = ARROW_UP,
                enabled = snapshot.canPush && !loading,
                trailing = pushTrailing,
                onClick = cb.onPush
            ),
            GitMenuAction(
                label = i18n("changes:menu.commit"),
                icon = COMMIT,
                enabled = hasChanges && hasMessage && !loading,
                onClick = cb.onCommit
            )
        )
    )
}

private fun advancedSection(
    snapshot: GitSnapshot,
    hasUpstream: Boolean,
    loading: Boolean,
    hasRemote: Boolean,
    hasChanges: Boolean,
    hasMessage: Boolean,
    hasBranches: Boolean,
    hasTags: Boolean,
    branchCount: Int,
    cb: ChangesMenuCallbacks
): GitMenuSection {
    val behind = snapshot.behindCount
    val ahead = snapshot.aheadCount
    val pushLabel = if (snapshot.needsPublish) i18n("changes:menu.push.publish") else i18n("changes:menu.push.push")
    val pushTrailing = if (!snapshot.needsPublish && ahead > 0) "↑$ahead" else null

    val pull = GitMenuSubmenu(
        label = i18n("changes:menu.pull.title"),
        icon = ARROW_DOWN,
        children = listOf(
            GitMenuAction(
                label = i18n("changes:menu.pull.pull"),
                icon = ARROW_DOWN,
                enabled = hasUpstream && !loading,
                trailing = if (behind > 0) "↓$behind" else null,
                onClick = cb.onPull
            ),
            GitMenuAction(
                label = i18n("changes:menu.pull.pull_from"),
                icon = ARROW_DOWN,
                enabled = hasRemote && !loading,
                onClick = cb.onPullFrom
            )
        )
    )

    val push = GitMenuSubmenu(
        label = i18n("changes:menu.push.title"),
        icon = ARROW_UP,
        children = listOf(
            GitMenuAction(
                label = pushLabel,
                icon = ARROW_UP,
                enabled = snapshot.canPush && !loading,
                trailing = pushTrailing,
                onClick = cb.onPush
            ),
            GitMenuAction(
                label = i18n("changes:menu.push.fetch"),
                icon = RELOAD,
                enabled = hasRemote && !loading,
                onClick = cb.onFetch
            ),
            GitMenuAction(
                label = i18n("changes:menu.push.amend"),
                icon = PENCIL,
                enabled = !loading && (hasChanges || hasMessage),
                onClick = cb.onAmend
            )
        )
    )

    val branches = GitMenuSubmenu(
        label = i18n("changes:menu.branch.title"),
        icon = BRANCH,
        children = listOf(
            GitMenuAction(
                label = i18n("changes:menu.branch.switch"),
                icon = BRANCH,
                enabled = hasBranches && !loading,
                onClick = cb.onCheckout
            ),
            GitMenuAction(
                label = i18n("changes:menu.branch.merge"),
                icon = BRANCH,
                enabled = branchCount > 1 && !loading,
                onClick = cb.onMergeBranch
            ),
            GitMenuAction(
                label = i18n("changes:menu.branch.rebase"),
                icon = BRANCH,
                enabled = branchCount > 1 && !loading,
                onClick = cb.onRebaseBranch
            ),
            GitMenuAction(
                label = i18n("changes:menu.branch.create"),
                icon = PLUS,
                enabled = !loading,
                onClick = cb.onCreateBranch
            ),
            GitMenuAction(
                label = i18n("changes:menu.branch.rename"),
                icon = PENCIL,
                enabled = hasBranches && !loading,
                onClick = cb.onRenameBranch
            ),
            GitMenuAction(
                label = i18n("changes:menu.branch.delete"),
                icon = TRASH,
                enabled = branchCount > 1 && !loading,
                destructive = true,
                onClick = cb.onDeleteBranch
            )
        )
    )

    val tags = GitMenuSubmenu(
        label = i18n("changes:menu.tag.title"),
        icon = TAG,
        children = listOf(
            GitMenuAction(
                label = i18n("changes:menu.tag.create"),
                icon = PLUS,
                enabled = !loading,
                onClick = cb.onCreateTag
            ),
            GitMenuAction(
                label = i18n("changes:menu.tag.delete"),
                icon = TRASH,
                enabled = hasTags && !loading,
                destructive = true,
                onClick = cb.onDeleteTag
            )
        )
    )

    return GitMenuSection(
        label = null,
        children = listOf(pull, push, branches, tags)
    )
}

private fun remoteSection(
    repo: Boolean,
    loading: Boolean,
    hasRemote: Boolean,
    hasRoot: Boolean,
    cb: ChangesMenuCallbacks
): GitMenuSection {
    val children = mutableListOf<GitMenuNode>()
    if (repo) {
        children += GitMenuSubmenu(
            label = i18n("changes:menu.remote.title"),
            icon = GLOBE,
            children = listOf(
                GitMenuAction(
                    label = i18n("changes:menu.remote.add"),
                    icon = PLUS,
                    enabled = !loading,
                    onClick = cb.onAddRemote
                ),
                GitMenuAction(
                    label = i18n("changes:menu.remote.remove"),
                    icon = TRASH,
                    enabled = hasRemote && !loading,
                    destructive = true,
                    onClick = cb.onRemoveRemote
                )
            )
        )
    } else {
        children += GitMenuAction(
            label = i18n("changes:layout.init_git"),
            icon = FOLDER,
            enabled = !loading && hasRoot,
            onClick = cb.onInit
        )
    }
    return GitMenuSection(
        label = i18n("changes:menu.section.repository"),
        children = children
    )
}

private fun i18n(key: String): String = I18n.get(key)
