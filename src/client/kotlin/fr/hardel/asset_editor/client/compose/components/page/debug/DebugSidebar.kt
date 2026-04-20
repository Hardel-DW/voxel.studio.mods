package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

private val ROW_SHAPE = RoundedCornerShape(8.dp)
private val BADGE_SHAPE = RoundedCornerShape(6.dp)

data class DebugSidebarEntry<K : Any>(
    val id: K,
    val icon: Identifier,
    val label: String,
    val description: String? = null,
    val count: Int? = null
)

@Composable
fun <K : Any> DebugSidebar(
    entries: List<DebugSidebarEntry<K>>,
    selectedId: K?,
    onSelect: (K) -> Unit,
    sectionLabel: String,
    modifier: Modifier = Modifier
) {
    val borderColor = StudioColors.Zinc800.copy(alpha = 0.5f)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxHeight()
            .background(StudioColors.Zinc950.copy(alpha = 0.4f))
            .drawBehind {
                val w = 1.dp.toPx()
                drawLine(borderColor, Offset(w / 2f, 0f), Offset(w / 2f, size.height), w)
                drawLine(borderColor, Offset(size.width - w / 2f, 0f), Offset(size.width - w / 2f, size.height), w)
            }
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp, horizontal = 10.dp)
    ) {
        Text(
            text = sectionLabel.uppercase(),
            style = StudioTypography.medium(10),
            color = StudioColors.Zinc600,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 4.dp)
        )
        entries.forEach { entry ->
            SidebarRow(
                entry = entry,
                selected = entry.id == selectedId,
                onClick = { onSelect(entry.id) }
            )
        }
    }
}

@Composable
private fun <K : Any> SidebarRow(
    entry: DebugSidebarEntry<K>,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val background by animateColorAsState(
        targetValue = when {
            selected -> StudioColors.Zinc800.copy(alpha = 0.6f)
            hovered -> StudioColors.Zinc800.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "debug-sidebar-bg"
    )
    val border by animateColorAsState(
        targetValue = when {
            selected -> StudioColors.Zinc700.copy(alpha = 0.7f)
            hovered -> StudioColors.Zinc700.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "debug-sidebar-border"
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            selected -> StudioColors.Zinc50
            hovered -> StudioColors.Zinc100
            else -> StudioColors.Zinc300
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "debug-sidebar-label"
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            selected -> StudioColors.Zinc100
            hovered -> StudioColors.Zinc200
            else -> StudioColors.Zinc500
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "debug-sidebar-icon"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(ROW_SHAPE)
            .background(background, ROW_SHAPE)
            .border(1.dp, border, ROW_SHAPE)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SvgIcon(location = entry.icon, size = 14.dp, tint = iconColor)
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.label,
                style = StudioTypography.medium(13),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.description.isNullOrBlank()) {
                Text(
                    text = entry.description,
                    style = StudioTypography.regular(10),
                    color = StudioColors.Zinc600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        entry.count?.let { CountBadge(it) }
    }
}

@Composable
private fun CountBadge(value: Int) {
    Box(
        modifier = Modifier
            .clip(BADGE_SHAPE)
            .background(StudioColors.Zinc400.copy(alpha = 0.12f))
            .border(1.dp, StudioColors.Zinc400.copy(alpha = 0.35f), BADGE_SHAPE)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = value.toString(),
            style = StudioTypography.semiBold(10),
            color = StudioColors.Zinc400,
            maxLines = 1
        )
    }
}
