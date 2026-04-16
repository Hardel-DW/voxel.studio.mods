package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeDiff
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.DiffStatus
import fr.hardel.asset_editor.client.compose.lib.git.GitFileStatus

@Composable
fun ChangesDiffBody(
    status: GitFileStatus,
    original: String,
    working: String,
    modifier: Modifier = Modifier
) {
    when (status) {
        GitFileStatus.ADDED, GitFileStatus.UNTRACKED -> CodeDiff(
            original = "",
            compiled = working,
            status = DiffStatus.ADDED,
            modifier = modifier
        )
        GitFileStatus.DELETED -> CodeDiff(
            original = original,
            compiled = "",
            status = DiffStatus.DELETED,
            modifier = modifier
        )
        GitFileStatus.MODIFIED, GitFileStatus.RENAMED -> CodeDiff(
            original = original,
            compiled = working,
            status = DiffStatus.UPDATED,
            modifier = modifier
        )
    }
}
