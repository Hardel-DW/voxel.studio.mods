package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import fr.hardel.asset_editor.client.compose.lib.git.OperationInProgress
import net.minecraft.client.resources.language.I18n

@Composable
fun ConflictBanner(
    snapshot: GitSnapshot,
    onContinue: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val operation = snapshot.operationInProgress
    if (operation == null && !snapshot.hasConflicts) return

    val conflictCount = snapshot.conflictedPaths.size
    val resolvable = conflictCount == 0 && !snapshot.isLoading
    val shape = RoundedCornerShape(10.dp)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(StudioColors.Red400.copy(alpha = 0.06f), shape)
            .border(1.dp, StudioColors.Red400.copy(alpha = 0.25f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = headline(operation, snapshot.currentBranch, snapshot.incomingBranch),
                style = StudioTypography.semiBold(12),
                color = StudioColors.Red300
            )
            Text(
                text = subtitle(conflictCount),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500
            )
        }

        if (operation != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAbort,
                    variant = ButtonVariant.GHOST_BORDER,
                    size = ButtonSize.SM,
                    text = I18n.get("changes:conflict.abort"),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                Button(
                    onClick = onContinue,
                    variant = ButtonVariant.DEFAULT,
                    size = ButtonSize.SM,
                    enabled = resolvable,
                    text = I18n.get("changes:conflict.continue"),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

private fun headline(
    operation: OperationInProgress?,
    current: String?,
    incoming: String?
): String {
    if (operation == null) return I18n.get("changes:conflict.standalone.headline")
    val key = when (operation) {
        OperationInProgress.MERGE -> "changes:conflict.merge.headline"
        OperationInProgress.REBASE -> "changes:conflict.rebase.headline"
        OperationInProgress.CHERRY_PICK -> "changes:conflict.cherry_pick.headline"
    }
    return I18n.get(key)
        .replace("{current}", current ?: "HEAD")
        .replace("{incoming}", incoming ?: "?")
}

private fun subtitle(count: Int): String {
    if (count == 0) return I18n.get("changes:conflict.ready")
    return I18n.get("changes:conflict.remaining").replace("{count}", count.toString())
}
