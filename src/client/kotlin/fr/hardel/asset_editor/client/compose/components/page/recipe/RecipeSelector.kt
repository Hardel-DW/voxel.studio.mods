package fr.hardel.asset_editor.client.compose.components.page.recipe

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private fun recipeTypeLabel(type: String): String =
    recipeTranslationBase(type)?.let { base -> I18n.get("$base.name") } ?: type

private fun recipeTranslationBase(type: String): String? =
    Identifier.tryParse(type)?.let { "recipe:crafting.${it.path}" }

@Composable
fun RecipeSelector(
    value: String,
    onChange: (String) -> Unit,
    recipeCounts: Map<String, Int>,
    selectMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    val displayEntryId =
        if (RecipeTreeData.isEntryId(value)) value else RecipeTreeData.getEntryByRecipeType(value).entryId.toString()
    val entryConfig = if (RecipeTreeData.isEntryId(value)) RecipeTreeData.getEntryConfig(value) else null
    val displayName = when {
        entryConfig != null -> I18n.get(entryConfig.translationKey)
        else -> recipeTypeLabel(value)
    }

    Box(modifier = modifier) {
        // TSX BoxHoveredTrigger
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(StudioColors.Zinc950, RoundedCornerShape(8.dp))
                .border(2.dp, StudioColors.Zinc800, RoundedCornerShape(8.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { expanded = !expanded }
        ) {
            Identifier.tryParse(displayEntryId)?.let { ItemSprite(it, 36.dp) }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { expanded = false; advancedExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 72.dp)
                        .background(StudioColors.Zinc950, RoundedCornerShape(12.dp))
                        .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(12.dp))
                ) {
                    ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.1f)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(64.dp)
                        ) {
                            Column {
                                Text(
                                    text = I18n.get("recipe:selector.title"),
                                    style = StudioTypography.bold(18),
                                    color = Color.White
                                )
                                Text(
                                    text = displayName,
                                    style = StudioTypography.regular(12),
                                    color = StudioColors.Zinc400
                                )
                            }

                            Identifier.tryParse(displayEntryId)?.let { ItemSprite(it, 48.dp) }
                        }

                        RecipeSelectorGrid(
                            currentValue = value,
                            recipeCounts = recipeCounts,
                            selectMode = selectMode,
                            onSelect = {
                                onChange(it)
                                expanded = false
                            }
                        )

                        Button(
                            onClick = { advancedExpanded = !advancedExpanded },
                            variant = ButtonVariant.GHOST_BORDER,
                            size = ButtonSize.SM,
                            text = I18n.get("recipe:selector.advanced")
                        )

                        if (advancedExpanded) {
                            val advancedTypes = RecipeTreeData.getAdvancedRecipeTypes()
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                advancedTypes.forEach { type ->
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
    selectMode: Boolean,
    onSelect: (String) -> Unit
) {
    val currentEntryId = if (RecipeTreeData.isEntryId(currentValue)) {
        currentValue
    } else {
        RecipeTreeData.getEntryByRecipeType(currentValue).entryId.toString()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecipeTreeData.getAllEntryIds(includeSpecial = true).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { entryId ->
                    val interaction = remember(entryId) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val count = recipeCounts[entryId] ?: 0
                    val enabled = selectMode || count > 0
                    val active = currentEntryId == entryId

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = when {
                                    active -> StudioColors.Zinc900
                                    hovered && enabled -> StudioColors.Zinc900
                                    else -> StudioColors.Zinc950
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                2.dp,
                                when {
                                    active -> StudioColors.Zinc800
                                    hovered && enabled -> StudioColors.Zinc800
                                    else -> StudioColors.Zinc900
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .hoverable(interaction)
                            .then(
                                if (enabled) {
                                    Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(
                                            interactionSource = interaction,
                                            indication = null
                                        ) { onSelect(entryId) }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Identifier.tryParse(entryId)?.let { ItemSprite(it, 32.dp) }

                        if (!selectMode || count > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(StudioColors.Zinc900, RoundedCornerShape(4.dp))
                                    .border(1.dp, StudioColors.Zinc600, RoundedCornerShape(4.dp))
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
            }
        }
    }
}

@Composable
private fun AdvancedTypeRow(label: String, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .background(
                color = if (hovered) StudioColors.Zinc900.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.medium(12),
            color = StudioColors.Zinc200
        )
        Text(
            text = description,
            style = StudioTypography.regular(10),
            color = StudioColors.Zinc500
        )
    }
}
