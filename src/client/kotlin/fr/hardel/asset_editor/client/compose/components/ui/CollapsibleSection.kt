package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import net.minecraft.resources.Identifier

private val CHEVRON_DOWN = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val cardShape = RoundedCornerShape(8.dp)

/**
 * Generic collapsible section with a clickable header (chevron + title + optional subtitle).
 * Content is animated via [standardCollapseEnter] / [standardCollapseExit] so motion stays
 * consistent with the rest of the design system.
 *
 * The header styling matches [EditorCard] so it slots into editor layouts without visual noise.
 */
@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = if (hovered || expanded) StudioColors.Zinc800 else StudioColors.Zinc900,
        animationSpec = StudioMotion.hoverSpec(),
        label = "collapsible-border"
    )
    val titleColor by animateColorAsState(
        targetValue = if (hovered || expanded) StudioColors.Zinc100 else StudioColors.Zinc300,
        animationSpec = StudioMotion.hoverSpec(),
        label = "collapsible-title"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = StudioMotion.hoverSpec(),
        label = "collapsible-chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, cardShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SvgIcon(
                location = CHEVRON_DOWN,
                size = 14.dp,
                tint = titleColor,
                modifier = Modifier.rotate(chevronRotation)
            )
            Spacer(Modifier.padding(start = 10.dp))
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = StudioTypography.medium(13),
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc500
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = standardCollapseEnter(),
            exit = standardCollapseExit()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                content = content
            )
        }
    }
}
