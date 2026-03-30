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
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private val ADVANCED_TYPES = linkedMapOf(
    "minecraft:crafting_shaped" to "recipe:crafting.crafting_shaped",
    "minecraft:crafting_shapeless" to "recipe:crafting.crafting_shapeless",
    "minecraft:crafting_transmute" to "recipe:crafting.crafting_transmute",
    "minecraft:smithing_transform" to "recipe:crafting.smithing_transform",
    "minecraft:smithing_trim" to "recipe:crafting.smithing_trim",
    "minecraft:crafting_special_repairitem" to "recipe:crafting.crafting_special_repairitem",
    "minecraft:crafting_special_mapcloning" to "recipe:crafting.crafting_special_mapcloning",
    "minecraft:crafting_special_mapextending" to "recipe:crafting.crafting_special_mapextending",
    "minecraft:crafting_special_shielddecoration" to "recipe:crafting.crafting_special_shielddecoration",
    "minecraft:crafting_decorated_pot" to "recipe:crafting.crafting_decorated_pot",
    "minecraft:crafting_special_firework_rocket" to "recipe:crafting.crafting_special_firework_rocket",
    "minecraft:crafting_special_firework_star" to "recipe:crafting.crafting_special_firework_star",
    "minecraft:crafting_special_firework_star_fade" to "recipe:crafting.crafting_special_firework_star_fade",
    "minecraft:crafting_special_tippedarrow" to "recipe:crafting.crafting_special_tippedarrow",
    "minecraft:crafting_special_armordye" to "recipe:crafting.crafting_special_armordye",
    "minecraft:crafting_special_bannerduplicate" to "recipe:crafting.crafting_special_bannerduplicate",
    "minecraft:crafting_special_bookcloning" to "recipe:crafting.crafting_special_bookcloning"
)

private fun recipeTypeLabel(type: String): String =
    ADVANCED_TYPES[type]?.let { key -> I18n.get("$key.name") } ?: type

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
    val displayBlockId =
        if (RecipeTreeData.isBlockId(value)) value else RecipeTreeData.getBlockByRecipeType(value).blockId.toString()
    val blockConfig = if (RecipeTreeData.isBlockId(value)) RecipeTreeData.getBlockConfig(value) else null
    val displayName = when {
        blockConfig?.special == true -> I18n.get("recipe:block.all")
        blockConfig != null -> StudioText.resolve("block", blockConfig.blockId)
        else -> recipeTypeLabel(value)
    }

    Box(modifier = modifier) {
        // TSX BoxHoveredTrigger
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(VoxelColors.Zinc950, RoundedCornerShape(8.dp))
                .border(2.dp, VoxelColors.Zinc800, RoundedCornerShape(8.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { expanded = !expanded }
        ) {
            Identifier.tryParse(displayBlockId)?.let { ItemSprite(it, 36.dp) }
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
                        .background(VoxelColors.Zinc950, RoundedCornerShape(12.dp))
                        .border(1.dp, VoxelColors.Zinc800, RoundedCornerShape(12.dp))
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
                                    style = VoxelTypography.bold(18),
                                    color = Color.White
                                )
                                Text(
                                    text = displayName,
                                    style = VoxelTypography.regular(12),
                                    color = VoxelColors.Zinc400
                                )
                            }

                            Identifier.tryParse(displayBlockId)?.let { ItemSprite(it, 48.dp) }
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
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                ADVANCED_TYPES.forEach { (type, key) ->
                                    AdvancedTypeRow(
                                        label = I18n.get("$key.name"),
                                        description = I18n.get("$key.description"),
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
    val currentBlockId = if (RecipeTreeData.isBlockId(currentValue)) {
        currentValue
    } else {
        RecipeTreeData.getBlockByRecipeType(currentValue).blockId.toString()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecipeTreeData.getAllBlockIds(includeSpecial = true).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { blockId ->
                    val interaction = remember(blockId) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val count = recipeCounts[blockId] ?: 0
                    val enabled = selectMode || count > 0
                    val active = currentBlockId == blockId

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = when {
                                    active -> VoxelColors.Zinc900
                                    hovered && enabled -> VoxelColors.Zinc900
                                    else -> VoxelColors.Zinc950
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                2.dp,
                                when {
                                    active -> VoxelColors.Zinc800
                                    hovered && enabled -> VoxelColors.Zinc800
                                    else -> VoxelColors.Zinc900
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
                                        ) { onSelect(blockId) }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Identifier.tryParse(blockId)?.let { ItemSprite(it, 32.dp) }

                        if (!selectMode || count > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(VoxelColors.Zinc900, RoundedCornerShape(4.dp))
                                    .border(1.dp, VoxelColors.Zinc600, RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = VoxelTypography.regular(9),
                                    color = VoxelColors.Zinc300,
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
                color = if (hovered) VoxelColors.Zinc900.copy(alpha = 0.6f) else Color.Transparent,
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
            style = VoxelTypography.medium(12),
            color = VoxelColors.Zinc200
        )
        Text(
            text = description,
            style = VoxelTypography.regular(10),
            color = VoxelColors.Zinc500
        )
    }
}
