package fr.hardel.asset_editor.client.compose.lib.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object GitCli {

    private const val DEFAULT_TIMEOUT_SECONDS = 60L

    @Volatile
    private var installedCache: Boolean? = null

    suspend fun isInstalled(): Boolean {
        installedCache?.let { return it }
        return withContext(Dispatchers.IO) {
            installedCache?.let { return@withContext it }
            val available = runCatching {
                val process = ProcessBuilder("git", "--version")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
            }.getOrElse { false }
            installedCache = available
            available
        }
    }

    suspend fun run(
        workingDir: Path,
        vararg args: String,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
    ): GitInvocation = withContext(Dispatchers.IO) {
        val command = buildList {
            add("git")
            addAll(args)
        }
        try {
            val process = ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(false)
                .also { it.environment()["LC_ALL"] = "C" }
                .start()

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext GitInvocation(GitInvocationOutcome.TIMEOUT, -1, "", "git timed out after ${timeoutSeconds}s")
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }
            GitInvocation(GitInvocationOutcome.COMPLETED, process.exitValue(), stdout, stderr)
        } catch (e: IOException) {
            installedCache = false
            GitInvocation(GitInvocationOutcome.NOT_INSTALLED, -1, "", e.message ?: "git executable not found")
        } catch (e: SecurityException) {
            GitInvocation(GitInvocationOutcome.NOT_INSTALLED, -1, "", e.message ?: "git execution denied")
        }
    }

    suspend fun runResult(workingDir: Path, vararg args: String): GitOpResult {
        val invocation = run(workingDir, *args)
        return when (invocation.outcome) {
            GitInvocationOutcome.NOT_INSTALLED -> GitOpResult.GitNotInstalled
            GitInvocationOutcome.TIMEOUT -> GitOpResult.Failure(invocation.stderr, invocation.exitCode)
            GitInvocationOutcome.COMPLETED -> {
                if (invocation.exitCode == 0) GitOpResult.Success
                else GitOpResult.Failure(invocation.stderr.ifBlank { invocation.stdout }, invocation.exitCode)
            }
        }
    }
}

enum class GitInvocationOutcome { COMPLETED, TIMEOUT, NOT_INSTALLED }

data class GitInvocation(
    val outcome: GitInvocationOutcome,
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = outcome == GitInvocationOutcome.COMPLETED && exitCode == 0
}
