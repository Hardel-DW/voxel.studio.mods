package fr.hardel.asset_editor.client.compose.lib.git

data class GitRemoteInfo(val owner: String, val repository: String) {

    fun compareUrl(base: String, head: String): String =
        "https://github.com/$owner/$repository/compare/$base...$head"

    companion object {
        private val SSH_REGEX = Regex("""git@github\.com:([^/]+)/([^/]+?)(?:\.git)?$""")
        private val HTTPS_REGEX = Regex("""https?://github\.com/([^/]+)/([^/]+?)(?:\.git)?/?$""")

        fun parse(remoteUrl: String?): GitRemoteInfo? {
            if (remoteUrl.isNullOrBlank()) return null
            val trimmed = remoteUrl.trim()
            SSH_REGEX.matchEntire(trimmed)?.let {
                return GitRemoteInfo(it.groupValues[1], it.groupValues[2])
            }
            HTTPS_REGEX.matchEntire(trimmed)?.let {
                return GitRemoteInfo(it.groupValues[1], it.groupValues[2])
            }
            return null
        }
    }
}
