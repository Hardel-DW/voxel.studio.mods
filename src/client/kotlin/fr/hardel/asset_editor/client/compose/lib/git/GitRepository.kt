package fr.hardel.asset_editor.client.compose.lib.git

import java.nio.file.Files
import java.nio.file.Path

class GitRepository(val root: Path) {

    suspend fun isRepository(): Boolean {
        if (!Files.isDirectory(root.resolve(".git"))) return false
        val invocation = GitCli.run(root, "rev-parse", "--is-inside-work-tree")
        return invocation.isSuccess && invocation.stdout.trim() == "true"
    }

    suspend fun init(initialBranch: String = "main", remoteUrl: String? = null): GitOpResult {
        val initResult = GitCli.runResult(root, "init", "-b", initialBranch)
        if (!initResult.isSuccess) return initResult
        if (remoteUrl.isNullOrBlank()) return GitOpResult.Success
        return setRemote("origin", remoteUrl.trim())
    }

    suspend fun setRemote(name: String, url: String): GitOpResult {
        val existing = GitCli.run(root, "remote", "get-url", name)
        return if (existing.isSuccess) {
            GitCli.runResult(root, "remote", "set-url", name, url)
        } else {
            GitCli.runResult(root, "remote", "add", name, url)
        }
    }

    suspend fun removeRemote(name: String): GitOpResult =
        GitCli.runResult(root, "remote", "remove", name)

    suspend fun listRemotes(): List<GitRemote> {
        val invocation = GitCli.run(root, "remote", "-v")
        if (!invocation.isSuccess) return emptyList()
        val seen = LinkedHashMap<String, String>()
        for (line in invocation.stdout.lineSequence()) {
            if (line.isBlank()) continue
            val tokens = line.split(Regex("\\s+"))
            if (tokens.size < 2) continue
            val name = tokens[0]
            val url = tokens[1]
            seen.putIfAbsent(name, url)
        }
        return seen.entries.map { GitRemote(it.key, it.value) }
    }

    suspend fun fetch(): GitOpResult = GitCli.runResult(root, "fetch", "--prune")

    suspend fun aheadBehind(): Pair<Int, Int>? {
        val invocation = GitCli.run(root, "rev-list", "--left-right", "--count", "HEAD...@{u}")
        if (!invocation.isSuccess) return null
        val parts = invocation.stdout.trim().split(Regex("\\s+"))
        if (parts.size != 2) return null
        val ahead = parts[0].toIntOrNull() ?: return null
        val behind = parts[1].toIntOrNull() ?: return null
        return ahead to behind
    }

    suspend fun currentBranch(): String? {
        val invocation = GitCli.run(root, "rev-parse", "--abbrev-ref", "HEAD")
        if (!invocation.isSuccess) return null
        val branch = invocation.stdout.trim()
        return branch.takeUnless { it.isBlank() || it == "HEAD" }
    }

    suspend fun branches(): List<String> {
        val invocation = GitCli.run(root, "for-each-ref", "--format=%(refname:short)", "refs/heads")
        if (!invocation.isSuccess) return emptyList()
        return invocation.stdout.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    suspend fun hasUpstream(): Boolean {
        val invocation = GitCli.run(root, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")
        return invocation.isSuccess
    }

    suspend fun remoteUrl(): String? {
        val invocation = GitCli.run(root, "config", "--get", "remote.origin.url")
        if (!invocation.isSuccess) return null
        return invocation.stdout.trim().takeUnless { it.isBlank() }
    }

    suspend fun status(): Map<String, GitFileStatus> {
        val invocation = GitCli.run(root, "status", "--porcelain=v1", "-uall")
        if (!invocation.isSuccess) return emptyMap()
        return parsePorcelain(invocation.stdout)
    }

    suspend fun readHead(path: String): String? {
        val invocation = GitCli.run(root, "show", "HEAD:$path")
        if (!invocation.isSuccess) return null
        return invocation.stdout
    }

    suspend fun commit(message: String, paths: List<String>): GitOpResult {
        if (paths.isEmpty()) return GitOpResult.Failure("No files to commit")
        val addArgs = buildList<String> {
            add("add")
            add("--")
            addAll(paths)
        }.toTypedArray()
        val addResult = GitCli.runResult(root, *addArgs)
        if (!addResult.isSuccess) return addResult
        return GitCli.runResult(root, "commit", "-m", message)
    }

    suspend fun push(setUpstream: Boolean = false): GitOpResult {
        if (!setUpstream) return GitCli.runResult(root, "push")
        val branch = currentBranch() ?: return GitOpResult.Failure("Detached HEAD, no branch to publish")
        return GitCli.runResult(root, "push", "-u", "origin", branch)
    }

    suspend fun pull(): GitOpResult = GitCli.runResult(root, "pull", "--ff-only")

    suspend fun createBranch(name: String): GitOpResult =
        GitCli.runResult(root, "checkout", "-b", name)

    suspend fun checkout(name: String): GitOpResult =
        GitCli.runResult(root, "checkout", name)

    suspend fun merge(name: String): GitOpResult =
        GitCli.runResult(root, "merge", "--no-ff", name)

    private fun parsePorcelain(output: String): Map<String, GitFileStatus> {
        val result = LinkedHashMap<String, GitFileStatus>()
        for (rawLine in output.lineSequence()) {
            if (rawLine.length < 3) continue
            val xy = rawLine.substring(0, 2)
            val rest = rawLine.substring(3).trim()
            val (path, status) = parseEntry(xy, rest) ?: continue
            result[path] = status
        }
        return result
    }

    private fun parseEntry(xy: String, rest: String): Pair<String, GitFileStatus>? {
        val path = when {
            xy.startsWith("R") || xy.startsWith("C") -> rest.substringAfter(" -> ", rest)
            else -> rest
        }.trim().trim('"')
        if (path.isEmpty()) return null

        val status = when {
            xy == "??" -> GitFileStatus.UNTRACKED
            xy.contains('D') -> GitFileStatus.DELETED
            xy.startsWith("A") || xy[1] == 'A' -> GitFileStatus.ADDED
            xy.startsWith("R") -> GitFileStatus.RENAMED
            xy.contains('M') -> GitFileStatus.MODIFIED
            else -> GitFileStatus.MODIFIED
        }
        return path to status
    }
}
