package fr.hardel.asset_editor.client.compose.lib.git

enum class GitFileStatus {
    ADDED,
    MODIFIED,
    DELETED,
    UNTRACKED,
    RENAMED;

    val isAddition: Boolean get() = this == ADDED || this == UNTRACKED

    val short: String get() = when (this) {
        ADDED, UNTRACKED -> "A"
        MODIFIED -> "M"
        DELETED -> "D"
        RENAMED -> "R"
    }
}
