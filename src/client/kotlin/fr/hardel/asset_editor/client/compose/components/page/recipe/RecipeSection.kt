package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.Counter
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.RecipeVisualModel
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeSection(
    model: RecipeVisualModel,
    selection: String,
    recipeCounts: Map<String, Int>,
    onSelectionChange: (String) -> Unit,
    onResultCountChange: (Int) -> Unit,
    onSlotPointerDown: (String, PointerButton) -> Unit,
    onSlotPointerEnter: (String) -> Unit,
    resultCountEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .border(1.dp, VoxelColors.Zinc900, RoundedCornerShape(12.dp))
    ) {
        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.12f)

        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = I18n.get("recipe:section.title"),
                        style = VoxelTypography.bold(20),
                        color = Color.White
                    )
                    Text(
                        text = I18n.get("recipe:section.description"),
                        style = VoxelTypography.regular(14),
                        color = VoxelColors.Zinc400
                    )
                }

                RecipeSelector(
                    value = selection,
                    onChange = onSelectionChange,
                    recipeCounts = recipeCounts,
                    selectMode = true
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                RecipeRenderer(
                    element = model,
                    interactive = true,
                    onSlotPointerDown = onSlotPointerDown,
                    onSlotPointerEnter = onSlotPointerEnter
                )

                Box(
                    modifier = Modifier
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .border(1.dp, VoxelColors.Zinc900, RoundedCornerShape(8.dp))
                ) {
                    ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.1f)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = I18n.get("recipe:section.result_count"),
                                    style = VoxelTypography.semiBold(16),
                                    color = VoxelColors.Zinc400
                                )
                                val descriptionKey = if (!resultCountEnabled && model.resultCountMax == 1) {
                                    "recipe:section.result_count_locked"
                                } else {
                                    "recipe:section.result_count_description"
                                }
                                Text(
                                    text = I18n.get(descriptionKey),
                                    style = VoxelTypography.regular(12),
                                    color = VoxelColors.Zinc500
                                )
                            }

                            Counter(
                                value = model.resultCount,
                                onValueChange = onResultCountChange,
                                min = 1,
                                max = model.resultCountMax,
                                step = 1,
                                enabled = resultCountEnabled
                            )
                        }

                    }
                }
            }
        }
    }
}
