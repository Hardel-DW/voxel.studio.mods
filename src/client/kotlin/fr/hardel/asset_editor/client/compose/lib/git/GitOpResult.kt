package fr.hardel.asset_editor.client.compose.lib.git

sealed class GitOpResult {
    data object Success : GitOpResult()
    data object GitNotInstalled : GitOpResult()
    data class Failure(val message: String, val exitCode: Int = -1) : GitOpResult()

    val isSuccess: Boolean get() = this is Success
    val errorMessage: String? get() = when (this) {
        is Success -> null
        is GitNotInstalled -> "Git is not installed on this machine"
        is Failure -> message.ifBlank { "git exited with code $exitCode" }
    }
}
