package fr.hardel.asset_editor.client.compose.routes.loot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import net.minecraft.client.resources.language.I18n

@Composable
fun LootTableOverviewPage(context: StudioContext) {
    val conceptUi = rememberConceptUi(context, StudioConcept.LOOT_TABLE)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            InputText(
                value = conceptUi.search,
                onValueChange = { value -> context.uiMemory().updateSearch(StudioConcept.LOOT_TABLE, value) },
                placeholder = I18n.get("loot:overview.search")
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = I18n.get("loot:overview.empty.title"),
                    style = VoxelTypography.medium(20),
                    color = VoxelColors.Zinc300
                )
                Text(
                    text = I18n.get("loot:overview.empty.description"),
                    style = VoxelTypography.regular(14),
                    color = VoxelColors.Zinc500
                )
            }
        }
    }
}
