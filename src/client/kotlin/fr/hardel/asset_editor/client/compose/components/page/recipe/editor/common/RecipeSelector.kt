package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.client.compose.PopupEnterAnimation
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val TRIGGER_SHAPE = RoundedCornerShape(8.dp)
private val POPUP_SHAPE = RoundedCornerShape(12.dp)
private val CELL_SHAPE = RoundedCornerShape(8.dp)
private val BADGE_SHAPE = RoundedCornerShape(4.dp)
private val ITEM_SHAPE = RoundedCornerShape(8.dp)

private const val DISMISS_DEBOUNCE_NS = 200_000_000L

private fun recipeTranslationBase(type: String): String? =
    Identifier.tryParse(type)?.let { "recipe:crafting.${it.path}" }

@Composable
fun RecipeSelector(
    value: String,
    onChange: (String) -> Unit,
    recipeCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var lastDismissNanos by remember { mutableStateOf(0L) }

    val resolvedEntry = RecipeTreeData.getEntryConfig(value)
        ?: RecipeTreeData.getEntryByRecipeType(value)
    val displayEntryId = resolvedEntry.entryId.toString()
    val displayName = I18n.get(resolvedEntry.translationKey)

    val triggerInteraction = remember { MutableInteractionSource() }
    val triggerHovered by triggerInteraction.collectIsHoveredAsState()
    val triggerBorderColor by animateColorAsState(
        targetValue = when {
            expanded -> StudioColors.Zinc600
            triggerHovered -> StudioColors.Zinc700
            else -> StudioColors.Zinc800
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "trigger-border"
    )

    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(StudioColors.Zinc950, TRIGGER_SHAPE)
                .border(2.dp, triggerBorderColor, TRIGGER_SHAPE)
                .hoverable(triggerInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = triggerInteraction,
                    indication = null
                ) {
                    if (System.nanoTime() - lastDismissNanos > DISMISS_DEBOUNCE_NS) {
                        expanded = !expanded
                        if (!expanded) advancedExpanded = false
                    }
                }
        ) {
            Identifier.tryParse(displayEntryId)?.let { ItemSprite(it, 36.dp) }
        }

        if (expanded) {
            val popupOffsetY = with(LocalDensity.current) { 72.dp.roundToPx() }

            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, popupOffsetY),
                onDismissRequest = {
                    expanded = false
                    advancedExpanded = false
                    lastDismissNanos = System.nanoTime()
                },
                properties = PopupProperties(focusable = true)
            ) {
                PopupEnterAnimation(
                    transformOrigin = TransformOrigin(1f, 0f),
                    modifier = Modifier.width(240.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(StudioColors.Zinc950, POPUP_SHAPE)
                            .border(1.dp, StudioColors.Zinc800, POPUP_SHAPE)
                            .clip(POPUP_SHAPE)
                    ) {
                        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.1f)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = I18n.get("recipe:selector.title"),
                                    style = StudioTypography.bold(18),
                                    color = Color.White
                                )
                                Text(
                                    text = displayName,
                                    style = StudioTypography.regular(12),
                                    color = StudioColors.Zinc400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Identifier.tryParse(displayEntryId)?.let { ItemSprite(it, 48.dp) }
                        }

                        RecipeSelectorGrid(
                            currentValue = value,
                            recipeCounts = recipeCounts,
                            onSelect = {
                                onChange(it)
                                expanded = false
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(1.dp)
                                .background(StudioColors.Zinc800.copy(alpha = 0.6f))
                        )

                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Button(
                                onClick = { advancedExpanded = !advancedExpanded },
                                variant = ButtonVariant.GHOST_BORDER,
                                size = ButtonSize.SM,
                                text = I18n.get("recipe:selector.advanced")
                            )
                        }

                        AnimatedVisibility(
                            visible = advancedExpanded,
                            enter = standardCollapseEnter(),
                            exit = standardCollapseExit()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .heightIn(max = 300.dp)
                                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                RecipeTreeData.getAdvancedRecipeTypes().forEach { type ->
                                    val translationBase = recipeTranslationBase(type) ?: return@forEach
                                    AdvancedTypeRow(
                                        label = I18n.get("$translationBase.name"),
                                        description = I18n.get("$translationBase.description"),
                                        onClick = {
                                            onChange(type)
                                            expanded = false
                                            advancedExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun RecipeSelectorGrid(
    currentValue: String,
    recipeCounts: Map<String, Int>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentEntryId = if (RecipeTreeData.isEntryId(currentValue)) {
        currentValue
    } else {
        RecipeTreeData.getEntryByRecipeType(currentValue).entryId.toString()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        RecipeTreeData.getAllEntryIds(includeSpecial = false).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { entryId ->
                    RecipeGridCell(
                        entryId = entryId,
                        count = recipeCounts[entryId] ?: 0,
                        active = currentEntryId == entryId,
                        onSelect = { onSelect(entryId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeGridCell(
    entryId: String,
    count: Int,
    active: Boolean,
    onSelect: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            active -> StudioColors.Zinc900
            hovered -> StudioColors.Zinc900
            else -> StudioColors.Zinc950
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "cell-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            active -> StudioColors.Zinc700
            hovered -> StudioColors.Zinc800
            else -> StudioColors.Zinc900
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "cell-border"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .background(backgroundColor, CELL_SHAPE)
            .border(2.dp, borderColor, CELL_SHAPE)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onSelect
            )
    ) {
        Identifier.tryParse(entryId)?.let { ItemSprite(it, 32.dp) }

        if (count > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 3.dp)
                    .background(StudioColors.Zinc900, BADGE_SHAPE)
                    .border(1.dp, StudioColors.Zinc600, BADGE_SHAPE)
            ) {
                Text(
                    text = count.toString(),
                    style = StudioTypography.regular(9),
                    color = StudioColors.Zinc300,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun AdvancedTypeRow(label: String, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc900.copy(alpha = 0.6f) else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "advanced-bg"
    )
    val labelColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc100 else StudioColors.Zinc200,
        animationSpec = StudioMotion.hoverSpec(),
        label = "advanced-label"
    )
    val descColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc400 else StudioColors.Zinc500,
        animationSpec = StudioMotion.hoverSpec(),
        label = "advanced-desc"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, ITEM_SHAPE)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.medium(12),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = description,
            style = StudioTypography.regular(10),
            color = descColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
