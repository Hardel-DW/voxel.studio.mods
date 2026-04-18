package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import fr.hardel.asset_editor.client.compose.lib.git.OperationInProgress
import net.minecraft.client.resources.language.I18n

@Composable
fun ConflictBanner(
    snapshot: GitSnapshot,
    onContinue: () -> Unit,
    onAbort: () -> Unit,
    onAcceptAllOurs: () -> Unit,
    onAcceptAllTheirs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val operation = snapshot.operationInProgress
    if (operation == null && !snapshot.hasConflicts) return

    val conflictCount = snapshot.conflictedPaths.size
    val resolvable = conflictCount == 0 && !snapshot.isLoading
    val bulkEnabled = conflictCount > 0 && !snapshot.isLoading
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

        if (bulkEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = I18n.get("changes:conflict.bulk.label"),
                    style = StudioTypography.medium(10),
                    color = StudioColors.Zinc500
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactBannerButton(
                        label = I18n.get("changes:conflict.accept_all.current.short"),
                        tint = StudioColors.Red400,
                        onClick = onAcceptAllOurs,
                        enabled = bulkEnabled,
                        modifier = Modifier.weight(1f)
                    )
                    CompactBannerButton(
                        label = I18n.get("changes:conflict.accept_all.incoming.short"),
                        tint = StudioColors.Red400,
                        onClick = onAcceptAllTheirs,
                        enabled = bulkEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (operation != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactBannerButton(
                    label = I18n.get("changes:conflict.abort"),
                    tint = StudioColors.Red400,
                    onClick = onAbort,
                    enabled = !snapshot.isLoading,
                    modifier = Modifier.weight(1f)
                )
                CompactBannerButton(
                    label = I18n.get("changes:conflict.continue"),
                    tint = StudioColors.Zinc300,
                    onClick = onContinue,
                    enabled = resolvable,
                    modifier = Modifier.weight(1f)
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

@Composable
private fun CompactBannerButton(
    label: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(6.dp)
    val bg by animateColorAsState(
        targetValue = if (isHovered && enabled) tint.copy(alpha = 0.12f) else tint.copy(alpha = 0.04f),
        animationSpec = tween(120),
        label = "compact-bg"
    )
    val border by animateColorAsState(
        targetValue = if (isHovered && enabled) tint.copy(alpha = 0.4f) else tint.copy(alpha = 0.22f),
        animationSpec = tween(120),
        label = "compact-border"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> StudioColors.Zinc600
            isHovered -> tint
            else -> StudioColors.Zinc300
        },
        animationSpec = tween(120),
        label = "compact-text"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .hoverable(interaction, enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.medium(11),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
