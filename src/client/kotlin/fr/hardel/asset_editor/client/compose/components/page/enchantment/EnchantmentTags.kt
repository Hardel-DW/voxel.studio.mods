package fr.hardel.asset_editor.client.compose.components.page.enchantment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.Popover
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SimpleCard
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.ToggleSwitch
import fr.hardel.asset_editor.client.compose.lib.utils.IconUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private const val MAX_DISPLAY = 3
private val STAR_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/star.svg")

@Composable
fun EnchantmentTags(
    title: String,
    description: String,
    imageId: Identifier?,
    values: List<String>,
    isTarget: Boolean,
    isMember: Boolean,
    locked: Boolean,
    onTargetToggle: (Boolean) -> Unit,
    onMembershipToggle: (Boolean) -> Unit,
    labelResolver: (String) -> String
) {
    var showOverflow by remember(title, values) { mutableStateOf(false) }
    var showActions by remember(title) { mutableStateOf(false) }
    val visibleValues = values.take(MAX_DISPLAY)
    val remainingValues = values.drop(MAX_DISPLAY)
    val seeMoreInteraction = remember(title, values) { MutableInteractionSource() }
    val seeMoreHovered by seeMoreInteraction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .alpha(if (locked) 0.5f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isTarget) {
                    Modifier.border(1.dp, StudioColors.Zinc600, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
    ) {
        SimpleCard(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            padding = PaddingValues(vertical = 16.dp, horizontal = 24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (imageId != null) {
                        if (IconUtils.isSvgIcon(imageId)) {
                            SvgIcon(imageId, 32.dp, StudioColors.Zinc300)
                        } else {
                            ResourceImageIcon(imageId, 32.dp)
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            style = StudioTypography.regular(16),
                            color = Color.White
                        )
                        Text(
                            text = description,
                            style = StudioTypography.light(12),
                            color = StudioColors.Zinc400
                        )
                    }
                }

                if (visibleValues.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StudioColors.Zinc700)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        visibleValues.forEach { value ->
                            TagChip(labelResolver(value))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (remainingValues.isNotEmpty()) {
                        Text(
                            text = "${I18n.get("generic:see.more")} (${remainingValues.size})",
                            style = StudioTypography.regular(12),
                            color = if (seeMoreHovered) StudioColors.Zinc200 else StudioColors.Zinc400,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .hoverable(seeMoreInteraction)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = seeMoreInteraction,
                                    indication = null
                                ) { showOverflow = true }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Popover(
                            expanded = showOverflow,
                            onDismiss = { showOverflow = false },
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(StudioColors.Zinc950, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                remainingValues.forEach { value ->
                                    TagChip(labelResolver(value))
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f))

                    Column {
                        Button(
                            onClick = { showActions = !showActions },
                            variant = ButtonVariant.GHOST_BORDER,
                            size = ButtonSize.SM,
                            text = I18n.get("generic:actions")
                        )
                        Popover(
                            expanded = showActions,
                            onDismiss = { showActions = false },
                            modifier = Modifier.width(320.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .background(StudioColors.Zinc950, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                ActionRow(
                                    title = I18n.get("enchantment:exclusive.actions.target.title"),
                                    subtitle = I18n.get("enchantment:exclusive.actions.target.subtitle"),
                                    description = I18n.get("enchantment:exclusive.actions.target.description"),
                                    checked = isTarget,
                                    enabled = !locked,
                                    onClick = { onTargetToggle(!isTarget) }
                                )
                                ActionRow(
                                    title = I18n.get("enchantment:exclusive.actions.membership.title"),
                                    subtitle = I18n.get("enchantment:exclusive.actions.membership.subtitle"),
                                    description = I18n.get("enchantment:exclusive.actions.membership.description"),
                                    checked = isMember,
                                    enabled = !locked,
                                    onClick = { onMembershipToggle(!isMember) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isMember) {
            SvgIcon(
                location = STAR_ICON,
                size = 16.dp,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
private fun TagChip(value: String) {
    Text(
        text = value,
        style = StudioTypography.regular(12),
        color = StudioColors.Zinc400,
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioColors.Zinc900.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .border(1.dp, StudioColors.Zinc900, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember(title, subtitle) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isHovered && enabled) {
                    Modifier.background(StudioColors.Zinc900.copy(alpha = 0.5f))
                } else {
                    Modifier
                }
            )
            .hoverable(interactionSource)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = StudioTypography.medium(13),
                    color = StudioColors.Zinc200
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = StudioTypography.regular(10),
                        color = StudioColors.Zinc500,
                        modifier = Modifier
                            .background(StudioColors.Zinc900.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, StudioColors.Zinc900, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = description,
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500
            )
        }

        ToggleSwitch(
            checked = checked,
            onCheckedChange = { onClick() },
            enabled = enabled
        )
    }
}
