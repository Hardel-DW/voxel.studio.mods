package fr.hardel.asset_editor.client.compose.lib.git

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import java.nio.file.Path

@Immutable
data class GitSnapshot(
    val root: Path?,
    val gitInstalled: Boolean,
    val isRepository: Boolean,
    val currentBranch: String?,
    val hasUpstream: Boolean,
    val aheadCount: Int,
    val behindCount: Int,
    val branches: ImmutableList<String>,
    val remoteUrl: String?,
    val remotes: ImmutableList<GitRemote>,
    val tags: ImmutableList<String>,
    val status: ImmutableMap<String, GitFileStatus>,
    val isLoading: Boolean,
    val pendingOp: GitOperationKind?,
    val lastError: String?
) {
    val hasChanges: Boolean get() = status.isNotEmpty()
    val canPush: Boolean get() = isRepository && currentBranch != null
    val needsPublish: Boolean get() = isRepository && currentBranch != null && !hasUpstream

    companion object {
        val Empty = GitSnapshot(
            root = null,
            gitInstalled = true,
            isRepository = false,
            currentBranch = null,
            hasUpstream = false,
            aheadCount = 0,
            behindCount = 0,
            branches = persistentListOf(),
            remoteUrl = null,
            remotes = persistentListOf(),
            tags = persistentListOf(),
            status = persistentMapOf(),
            isLoading = false,
            pendingOp = null,
            lastError = null
        )
    }
}

enum class GitOperationKind {
    REFRESH,
    FETCH,
    INIT,
    ADD_REMOTE,
    COMMIT,
    AMEND,
    PUSH,
    PUBLISH,
    PULL,
    PULL_FROM,
    BRANCH_CREATE,
    BRANCH_CHECKOUT,
    BRANCH_DELETE,
    BRANCH_RENAME,
    TAG_CREATE,
    TAG_DELETE,
    MERGE,
    PR
}
