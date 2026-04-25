package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureViewerPage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = I18n.get("structure:mode.structure.title"),
            style = StudioTypography.semiBold(22),
            color = StudioColors.Zinc100
        )
        Text(
            text = destination?.elementId ?: I18n.get("structure:viewer.empty"),
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc400,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = I18n.get("structure:mode.structure.description"),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
