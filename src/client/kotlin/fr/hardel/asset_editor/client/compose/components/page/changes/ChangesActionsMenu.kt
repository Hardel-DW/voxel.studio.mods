package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Popover
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")

@Composable
fun ChangesActionsMenu(
    expanded: Boolean,
    snapshot: GitSnapshot,
    currentView: String,
    onDismiss: () -> Unit,
    onViewChange: (String) -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onCommit: () -> Unit,
    onCheckout: () -> Unit,
    onCreateBranch: () -> Unit,
    onAddRemote: () -> Unit,
    onRemoveRemote: () -> Unit,
    onInit: () -> Unit
) {
    Popover(
        expanded = expanded,
        onDismiss = onDismiss,
        modifier = Modifier.width(240.dp),
        alignment = Alignment.TopStart,
        offset = IntOffset(x = 0, y = 30)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MainMenu(
                snapshot = snapshot,
                currentView = currentView,
                dismiss = onDismiss,
                onViewChange = onViewChange,
                onPull = onPull,
                onPush = onPush,
                onCommit = onCommit,
                onOpenCheckout = onCheckout,
                onCreateBranch = onCreateBranch,
                onAddRemote = onAddRemote,
                onRemoveRemote = onRemoveRemote,
                onInit = onInit
            )
        }
    }
}

@Composable
private fun MainMenu(
    snapshot: GitSnapshot,
    currentView: String,
    dismiss: () -> Unit,
    onViewChange: (String) -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onCommit: () -> Unit,
    onOpenCheckout: () -> Unit,
    onCreateBranch: () -> Unit,
    onAddRemote: () -> Unit,
    onRemoveRemote: () -> Unit,
    onInit: () -> Unit
) {
    SectionLabel(I18n.get("changes:menu.section.view"))
    MenuItem(
        label = I18n.get("changes:view.concept"),
        checked = currentView == ChangesView.CONCEPT,
        onClick = {
            onViewChange(ChangesView.CONCEPT)
            dismiss()
        }
    )
    MenuItem(
        label = I18n.get("changes:view.file"),
        checked = currentView == ChangesView.FILE,
        onClick = {
            onViewChange(ChangesView.FILE)
            dismiss()
        }
    )

    Divider()
    SectionLabel(I18n.get("changes:menu.section.actions"))
    MenuItem(
        label = I18n.get("github:primary.pull"),
        enabled = snapshot.isRepository && snapshot.hasUpstream && !snapshot.isLoading,
        onClick = {
            onPull()
            dismiss()
        }
    )
    MenuItem(
        label = if (snapshot.needsPublish) I18n.get("github:primary.publish") else I18n.get("github:primary.push"),
        enabled = snapshot.canPush && !snapshot.isLoading,
        onClick = {
            onPush()
            dismiss()
        }
    )
    MenuItem(
        label = I18n.get("github:primary.commit"),
        enabled = snapshot.hasChanges && !snapshot.isLoading,
        onClick = {
            onCommit()
            dismiss()
        }
    )
    MenuItem(
        label = I18n.get("changes:menu.checkout"),
        enabled = snapshot.isRepository && snapshot.branches.isNotEmpty(),
        onClick = onOpenCheckout,
        trailingChevron = true
    )
    MenuItem(
        label = I18n.get("changes:menu.create_branch"),
        enabled = snapshot.isRepository && !snapshot.isLoading,
        onClick = {
            onCreateBranch()
            dismiss()
        }
    )

    Divider()
    SectionLabel(I18n.get("changes:menu.section.repository"))
    MenuItem(
        label = I18n.get("changes:menu.add_remote"),
        enabled = snapshot.isRepository && !snapshot.isLoading,
        onClick = {
            onAddRemote()
            dismiss()
        }
    )
    MenuItem(
        label = I18n.get("changes:menu.remove_remote"),
        enabled = snapshot.isRepository && !snapshot.isLoading && snapshot.remotes.isNotEmpty(),
        onClick = {
            onRemoveRemote()
            dismiss()
        }
    )
    if (!snapshot.isRepository) {
        MenuItem(
            label = I18n.get("changes:layout.init_git"),
            enabled = !snapshot.isLoading && snapshot.root != null,
            onClick = {
                onInit()
                dismiss()
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = StudioTypography.bold(9),
        color = StudioColors.Zinc600,
        modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 4.dp)
    )
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(StudioColors.Zinc800.copy(alpha = 0.6f))
    )
}

@Composable
private fun MenuItem(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    checked: Boolean = false,
    trailingChevron: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(6.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (enabled && isHovered) StudioColors.Zinc800.copy(alpha = 0.7f) else Color.Transparent
            )
            .hoverable(interaction, enabled = enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Box(modifier = Modifier.width(14.dp)) {
            if (checked) {
                SvgIcon(location = CHECK_ICON, size = 12.dp, tint = Color.White)
            }
        }
        Text(
            text = label,
            style = StudioTypography.regular(12),
            color = when {
                !enabled -> StudioColors.Zinc700
                isHovered -> Color.White
                else -> StudioColors.Zinc300
            },
            modifier = Modifier.weight(1f)
        )
        if (trailingChevron) {
            Text(
                text = "›",
                style = StudioTypography.regular(14),
                color = StudioColors.Zinc500
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}
