package fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceEmptyState
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceHeader
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioTabEntry
import fr.hardel.asset_editor.client.compose.lib.rememberActiveTabId
import fr.hardel.asset_editor.client.compose.lib.rememberOpenTabs
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val TABLE_HEAD_SHAPE = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
private val TABLE_BODY_SHAPE = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
private val FOCUS_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-right.svg")
private val CLOSE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/close.svg")
private const val WEIGHT_INDEX = 0.3f
private const val WEIGHT_CONCEPT = 1f
private const val WEIGHT_ELEMENT = 2f
private const val WEIGHT_TAB = 1f
private const val WEIGHT_ACTIONS = 0.7f

@Composable
fun DebugWorkspaceNavigationPanel(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    val tabs = rememberOpenTabs(context)
    val activeTabId = rememberActiveTabId(context)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        DebugWorkspaceHeader(
            title = I18n.get("debug:workspace.panel.navigation.title"),
            subtitle = I18n.get("debug:workspace.panel.navigation.subtitle")
        )

        if (tabs.isEmpty()) {
            DebugWorkspaceEmptyState(
                title = I18n.get("debug:workspace.navigation.tabs.empty.title"),
                subtitle = I18n.get("debug:workspace.navigation.tabs.empty.subtitle")
            )
            return@Column
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            TableHead()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TABLE_BODY_SHAPE)
                    .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), TABLE_BODY_SHAPE)
                    .heightIn(max = 800.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { index, entry ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(StudioColors.Zinc800.copy(alpha = 0.3f))
                        )
                    }
                    TabRow(
                        index = index,
                        entry = entry,
                        isEven = index % 2 == 0,
                        active = entry.tabId == activeTabId,
                        onFocus = { context.navigationMemory().switchTab(entry.tabId) },
                        onClose = { context.navigationMemory().closeTab(entry.tabId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHead() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(StudioColors.Zinc900.copy(alpha = 0.4f), TABLE_HEAD_SHAPE)
            .padding(horizontal = 16.dp)
    ) {
        HeaderCell(I18n.get("debug:workspace.navigation.column.index"), WEIGHT_INDEX)
        HeaderCell(I18n.get("debug:workspace.navigation.column.concept"), WEIGHT_CONCEPT)
        HeaderCell(I18n.get("debug:workspace.navigation.column.element"), WEIGHT_ELEMENT)
        HeaderCell(I18n.get("debug:workspace.navigation.column.tab"), WEIGHT_TAB)
        HeaderCell(I18n.get("debug:workspace.navigation.column.actions"), WEIGHT_ACTIONS)
    }
}

@Composable
private fun RowScope.HeaderCell(label: String, weight: Float) {
    Box(modifier = Modifier.weight(weight)) {
        Text(
            text = label.uppercase(),
            style = StudioTypography.medium(11),
            color = StudioColors.Zinc500
        )
    }
}

@Composable
private fun TabRow(
    index: Int,
    entry: StudioTabEntry,
    isEven: Boolean,
    active: Boolean,
    onFocus: () -> Unit,
    onClose: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val background by animateColorAsState(
        targetValue = when {
            active -> StudioColors.Zinc100.copy(alpha = 0.08f)
            hovered -> StudioColors.Zinc800.copy(alpha = 0.4f)
            isEven -> StudioColors.Zinc900.copy(alpha = 0.15f)
            else -> StudioColors.Zinc950.copy(alpha = 0.3f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "nav-row-bg"
    )
    val textColor = if (active) StudioColors.Zinc50 else StudioColors.Zinc300

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(background)
            .hoverable(interaction)
            .padding(horizontal = 16.dp)
    ) {
        Box(modifier = Modifier.weight(WEIGHT_INDEX)) {
            Text(
                "#${index + 1}",
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500
            )
        }
        Box(modifier = Modifier.weight(WEIGHT_CONCEPT)) {
            Text(
                entry.destination.conceptId.path,
                style = StudioTypography.regular(12),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.weight(WEIGHT_ELEMENT)) {
            Text(
                entry.destination.elementId,
                style = StudioTypography.regular(12),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.weight(WEIGHT_TAB)) {
            Text(
                entry.destination.tabId.path,
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(WEIGHT_ACTIONS)
        ) {
            IconAction(
                icon = FOCUS_ICON,
                onClick = onFocus,
                enabled = !active
            )
            IconAction(
                icon = CLOSE_ICON,
                onClick = onClose,
                enabled = true
            )
        }
    }
}

@Composable
private fun IconAction(
    icon: Identifier,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint: Color = when {
        !enabled -> StudioColors.Zinc700
        hovered -> StudioColors.Zinc100
        else -> StudioColors.Zinc400
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .hoverable(interaction)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        SvgIcon(location = icon, size = 13.dp, tint = tint)
    }
}
