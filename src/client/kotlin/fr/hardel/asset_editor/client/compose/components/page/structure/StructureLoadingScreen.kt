package fr.hardel.asset_editor.client.compose.components.page.structure

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
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

/** Awaiting-assembly screen shown while the server-side snapshot for a worldgen structure is in flight. */
@Composable
fun StructureLoadingScreen(structureId: Identifier, info: StructureWorldgenSnapshot?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(structureId.toString(), style = StudioTypography.semiBold(18), color = StudioColors.Zinc100)
        info?.let {
            Text(
                text = it.type(),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Text(
            text = I18n.get("structure:viewer.assembling"),
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc400,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
