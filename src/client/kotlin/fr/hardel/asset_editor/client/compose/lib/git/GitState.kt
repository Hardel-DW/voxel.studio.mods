package fr.hardel.asset_editor.client.compose.lib.git

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberSelectedPack
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path

@Stable
class GitState internal constructor(
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var repository: GitRepository? = null

    var snapshot by mutableStateOf(GitSnapshot.Empty)
        private set

    fun bind(root: Path?) {
        if (root == null) {
            repository = null
            snapshot = GitSnapshot.Empty
            return
        }
        if (repository?.root == root) return
        repository = GitRepository(root)
        snapshot = GitSnapshot.Empty.copy(root = root)
        refresh()
    }

    fun refresh() {
        val repo = repository ?: return
        scope.launch {
            mutex.withLock {
                snapshot = snapshot.copy(isLoading = true, pendingOp = GitOperationKind.REFRESH, lastError = null)
                runCatching { refreshLocked(repo) }.onFailure { error ->
                    snapshot = snapshot.copy(
                        isLoading = false,
                        pendingOp = null,
                        lastError = error.message ?: error::class.simpleName ?: "unknown error"
                    )
                }
            }
        }
    }

    suspend fun readDiff(path: String): GitDiffPayload {
        val repo = repository ?: return GitDiffPayload.Empty
        val root = repo.root
        val absolute = root.resolve(path)
        val working = runCatching {
            if (java.nio.file.Files.exists(absolute)) java.nio.file.Files.readString(absolute) else ""
        }.getOrElse { "" }
        val original = repo.readHead(path) ?: ""
        return GitDiffPayload(original = original, working = working)
    }

    fun init(remoteUrl: String? = null) =
        launchOp(GitOperationKind.INIT) { repo -> repo.init(remoteUrl = remoteUrl) }

    fun setRemote(url: String) =
        launchOp(GitOperationKind.ADD_REMOTE) { repo -> repo.setRemote("origin", url) }

    fun addRemote(name: String, url: String) =
        launchOp(GitOperationKind.ADD_REMOTE) { repo -> repo.setRemote(name, url) }

    fun removeRemote(name: String) =
        launchOp(GitOperationKind.ADD_REMOTE) { repo -> repo.removeRemote(name) }

    fun fetchAndRefresh() {
        val repo = repository ?: return
        scope.launch {
            mutex.withLock {
                snapshot = snapshot.copy(isLoading = true, pendingOp = GitOperationKind.FETCH, lastError = null)
                if (snapshot.isRepository && snapshot.remoteUrl != null) {
                    val result = runCatching { repo.fetch() }.getOrElse { error ->
                        GitOpResult.Failure(error.message ?: error::class.simpleName ?: "unknown error")
                    }
                    if (!result.isSuccess) {
                        snapshot = snapshot.copy(
                            isLoading = false,
                            pendingOp = null,
                            lastError = result.errorMessage
                        )
                        return@withLock
                    }
                }
                runCatching { refreshLocked(repo) }.onFailure { error ->
                    snapshot = snapshot.copy(
                        isLoading = false,
                        pendingOp = null,
                        lastError = error.message ?: error::class.simpleName ?: "unknown error"
                    )
                }
            }
        }
    }

    fun commit(message: String, paths: List<String>) =
        launchOp(GitOperationKind.COMMIT) { repo -> repo.commit(message, paths) }

    fun push() {
        val current = snapshot
        val kind = if (current.needsPublish) GitOperationKind.PUBLISH else GitOperationKind.PUSH
        launchOp(kind) { repo -> repo.push(setUpstream = current.needsPublish) }
    }

    fun pull() = launchOp(GitOperationKind.PULL) { repo -> repo.pull() }

    fun pullRebase() = launchOp(GitOperationKind.PULL) { repo -> repo.pullRebase() }

    fun pullFrom(remote: String, branch: String) =
        launchOp(GitOperationKind.PULL_FROM) { repo -> repo.pullFrom(remote, branch) }

    fun fetch() = launchOp(GitOperationKind.FETCH) { repo -> repo.fetch() }

    fun amend(message: String?) =
        launchOp(GitOperationKind.AMEND) { repo -> repo.amendCommit(message) }

    fun createBranch(name: String) =
        launchOp(GitOperationKind.BRANCH_CREATE) { repo -> repo.createBranch(name) }

    fun checkoutBranch(name: String) =
        launchOp(GitOperationKind.BRANCH_CHECKOUT) { repo -> repo.checkout(name) }

    fun deleteBranch(name: String, force: Boolean = false) =
        launchOp(GitOperationKind.BRANCH_DELETE) { repo -> repo.deleteBranch(name, force) }

    fun renameBranch(oldName: String, newName: String) =
        launchOp(GitOperationKind.BRANCH_RENAME) { repo -> repo.renameBranch(oldName, newName) }

    fun createTag(name: String, message: String?) =
        launchOp(GitOperationKind.TAG_CREATE) { repo -> repo.createTag(name, message) }

    fun deleteTag(name: String) =
        launchOp(GitOperationKind.TAG_DELETE) { repo -> repo.deleteTag(name) }

    fun merge(name: String) =
        launchOp(GitOperationKind.MERGE) { repo -> repo.merge(name) }

    fun rebase(name: String) =
        launchOp(GitOperationKind.REBASE) { repo -> repo.rebase(name) }

    /**
     * During rebase git swaps the semantic of stages: `:2:` becomes the upstream/onto content
     * and `:3:` becomes the commit being replayed (i.e. the user's own work). To keep the UI
     * consistent with the user's mental model of "current = my side, incoming = upstream",
     * we flip the stage mapping based on the operation in progress.
     */
    private fun currentStage(): Int =
        if (snapshot.operationInProgress == OperationInProgress.REBASE) 3 else 2

    private fun incomingStage(): Int =
        if (snapshot.operationInProgress == OperationInProgress.REBASE) 2 else 3

    suspend fun readCurrentSide(path: String): String? =
        repository?.readConflictStage(path, currentStage())

    suspend fun readIncomingSide(path: String): String? =
        repository?.readConflictStage(path, incomingStage())

    fun acceptCurrent(path: String) {
        val rebasing = snapshot.operationInProgress == OperationInProgress.REBASE
        launchOp(GitOperationKind.CONFLICT_RESOLVE) { repo ->
            if (rebasing) repo.acceptTheirs(path) else repo.acceptOurs(path)
        }
    }

    fun acceptIncoming(path: String) {
        val rebasing = snapshot.operationInProgress == OperationInProgress.REBASE
        launchOp(GitOperationKind.CONFLICT_RESOLVE) { repo ->
            if (rebasing) repo.acceptOurs(path) else repo.acceptTheirs(path)
        }
    }

    fun acceptAllCurrent() {
        val paths = snapshot.conflictedPaths.toList()
        if (paths.isEmpty()) return
        val rebasing = snapshot.operationInProgress == OperationInProgress.REBASE
        launchOp(GitOperationKind.CONFLICT_RESOLVE) { repo ->
            bulkAccept(paths) { if (rebasing) repo.acceptTheirs(it) else repo.acceptOurs(it) }
        }
    }

    fun acceptAllIncoming() {
        val paths = snapshot.conflictedPaths.toList()
        if (paths.isEmpty()) return
        val rebasing = snapshot.operationInProgress == OperationInProgress.REBASE
        launchOp(GitOperationKind.CONFLICT_RESOLVE) { repo ->
            bulkAccept(paths) { if (rebasing) repo.acceptOurs(it) else repo.acceptTheirs(it) }
        }
    }

    private suspend fun bulkAccept(paths: List<String>, action: suspend (String) -> GitOpResult): GitOpResult {
        for (path in paths) {
            val result = action(path)
            if (!result.isSuccess) return result
        }
        return GitOpResult.Success
    }

    fun continueOperation() {
        val op = snapshot.operationInProgress ?: return
        launchOp(GitOperationKind.CONFLICT_RESOLVE) { repo ->
            when (op) {
                OperationInProgress.MERGE -> repo.mergeContinue()
                OperationInProgress.REBASE -> repo.rebaseContinue()
                OperationInProgress.CHERRY_PICK -> GitOpResult.Failure("Cherry-pick continue not supported")
            }
        }
    }

    fun abortOperation() {
        val op = snapshot.operationInProgress ?: return
        launchOp(GitOperationKind.CONFLICT_RESOLVE) { repo ->
            when (op) {
                OperationInProgress.MERGE -> repo.mergeAbort()
                OperationInProgress.REBASE -> repo.rebaseAbort()
                OperationInProgress.CHERRY_PICK -> GitOpResult.Failure("Cherry-pick abort not supported")
            }
        }
    }

    fun clearError() {
        if (snapshot.lastError != null) snapshot = snapshot.copy(lastError = null)
    }

    private fun launchOp(
        kind: GitOperationKind,
        refreshAfter: Boolean = true,
        block: suspend (GitRepository) -> GitOpResult
    ) {
        val repo = repository ?: return
        scope.launch {
            mutex.withLock {
                snapshot = snapshot.copy(isLoading = true, pendingOp = kind, lastError = null)
                val result = runCatching { block(repo) }.getOrElse { error ->
                    GitOpResult.Failure(error.message ?: error::class.simpleName ?: "unknown error")
                }
                if (refreshAfter) {
                    runCatching { refreshLocked(repo) }.onFailure { error ->
                        snapshot = snapshot.copy(
                            isLoading = false,
                            pendingOp = null,
                            lastError = error.message ?: error::class.simpleName ?: "unknown error"
                        )
                        return@withLock
                    }
                } else {
                    snapshot = snapshot.copy(isLoading = false, pendingOp = null)
                }
                if (!result.isSuccess) {
                    snapshot = snapshot.copy(lastError = result.errorMessage)
                }
            }
        }
    }

    private suspend fun refreshLocked(repo: GitRepository) {
        val installed = GitCli.isInstalled()
        if (!installed) {
            snapshot = snapshot.copy(
                gitInstalled = false,
                isRepository = false,
                isLoading = false,
                pendingOp = null
            )
            return
        }
        val isRepo = repo.isRepository()
        if (!isRepo) {
            snapshot = snapshot.copy(
                gitInstalled = true,
                isRepository = false,
                currentBranch = null,
                hasUpstream = false,
                aheadCount = 0,
                behindCount = 0,
                branches = emptyList<String>().toImmutableList(),
                remoteUrl = null,
                remotes = emptyList<GitRemote>().toImmutableList(),
                tags = emptyList<String>().toImmutableList(),
                status = emptyMap<String, GitFileStatus>().toImmutableMap(),
                isLoading = false,
                pendingOp = null
            )
            return
        }
        val branch = repo.currentBranch()
        val upstream = repo.hasUpstream()
        val branches = repo.branches()
        val remote = repo.remoteUrl()
        val remotes = repo.listRemotes()
        val tags = repo.tags()
        val status = repo.status()
        val counts = if (upstream) repo.aheadBehind() else null
        val operation = repo.operationInProgress()
        val incoming = operation?.let { repo.incomingBranch(it) }
        val conflicted = status.entries.filter { it.value == GitFileStatus.CONFLICTED }.map { it.key }
        if (conflicted.isNotEmpty()) {
            runCatching { repo.neutralizeConflictMarkers(conflicted) }
        }
        snapshot = snapshot.copy(
            gitInstalled = true,
            isRepository = true,
            currentBranch = branch,
            hasUpstream = upstream,
            aheadCount = counts?.first ?: 0,
            behindCount = counts?.second ?: 0,
            branches = branches.toImmutableList(),
            remoteUrl = remote,
            remotes = remotes.toImmutableList(),
            tags = tags.toImmutableList(),
            status = status.toImmutableMap(),
            operationInProgress = operation,
            incomingBranch = incoming,
            isLoading = false,
            pendingOp = null
        )
    }
}

data class GitDiffPayload(val original: String, val working: String) {
    companion object {
        val Empty = GitDiffPayload("", "")
    }
}

@Composable
fun rememberGitState(context: StudioContext): GitState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { GitState(scope) }
    val packId = rememberSelectedPack(context)?.packId()
    LaunchedEffect(packId) {
        state.bind(packId?.let { PackRootResolver.resolveFromPackId(it) })
    }
    return state
}
