package fr.hardel.asset_editor.client.compose.components.page.changes

import fr.hardel.asset_editor.client.compose.lib.git.GitOperationKind
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n

enum class ChangesPrimaryAction {
    INIT,
    PULL,
    COMMIT,
    PUSH,
    PUBLISH,
    NONE;

    fun label(snapshot: GitSnapshot): String = when (this) {
        INIT -> pendingLabel(snapshot, GitOperationKind.INIT)
            ?: I18n.get("changes:layout.init_git")
        PULL -> pendingLabel(snapshot, GitOperationKind.PULL)
            ?: I18n.get("github:primary.pull")
        COMMIT -> pendingLabel(snapshot, GitOperationKind.COMMIT)
            ?: I18n.get("github:primary.commit")
        PUSH -> pendingLabel(snapshot, GitOperationKind.PUSH)
            ?: I18n.get("github:primary.push")
        PUBLISH -> pendingLabel(snapshot, GitOperationKind.PUBLISH)
            ?: I18n.get("github:primary.publish")
        NONE -> I18n.get("github:primary.no_action")
    }

    companion object {
        fun resolve(snapshot: GitSnapshot): ChangesPrimaryAction {
            if (!snapshot.gitInstalled || snapshot.root == null) return NONE
            if (!snapshot.isRepository) return INIT
            if (snapshot.currentBranch == null) return NONE
            if (snapshot.behindCount > 0) return PULL
            if (snapshot.status.isNotEmpty()) return COMMIT
            if (snapshot.needsPublish) return PUBLISH
            if (snapshot.aheadCount > 0) return PUSH
            return NONE
        }

        private fun pendingLabel(snapshot: GitSnapshot, forOp: GitOperationKind): String? {
            if (!snapshot.isLoading || snapshot.pendingOp != forOp) return null
            return when (forOp) {
                GitOperationKind.INIT -> I18n.get("github:primary.init.pending")
                GitOperationKind.PULL -> I18n.get("github:primary.pull.pending")
                GitOperationKind.COMMIT -> I18n.get("github:primary.commit.pending")
                GitOperationKind.PUSH -> I18n.get("github:primary.push.pending")
                GitOperationKind.PUBLISH -> I18n.get("github:primary.publish.pending")
                else -> null
            }
        }
    }
}
