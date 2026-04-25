package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.structure.STRUCTURE_CONCEPT_ID
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureOverviewPage(context: StudioContext, templates: List<StructureTemplateSnapshot>) {
    val conceptId = STRUCTURE_CONCEPT_ID
    val conceptUi = rememberConceptUi(context, conceptId)
    val search = conceptUi.search
    val visible = remember(templates, search) {
        templates.filter { search.isBlank() || it.id().toString().contains(search, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(I18n.get("structure:overview.title"), style = StudioTypography.semiBold(28), color = StudioColors.Zinc100)
                Text(I18n.get("structure:overview.subtitle", templates.size), style = StudioTypography.regular(13), color = StudioColors.Zinc500)
            }
            InputText(
                value = search,
                onValueChange = { context.uiMemory().updateSearch(conceptId, it) },
                placeholder = I18n.get("structure:overview.search"),
                maxWidth = 360.dp,
                focusExpand = false
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visible, key = { it.id().toString() }) { template ->
                StructureOverviewRow(
                    template = template,
                    onClick = {
                        context.navigationMemory().openElement(
                            ElementEditorDestination(conceptId, template.id().toString(), context.studioDefaultEditorTab(conceptId))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun StructureOverviewRow(template: StructureTemplateSnapshot, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(shape)
            .background(StudioColors.Zinc900.copy(alpha = 0.55f))
            .border(1.dp, StudioColors.Zinc800, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor(template.id().toString()).copy(alpha = 0.22f))
                .border(1.dp, accentColor(template.id().toString()).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(template.id().toString(), style = StudioTypography.semiBold(14), color = StudioColors.Zinc100)
            Text(template.sourcePack(), style = StudioTypography.regular(11), color = StudioColors.Zinc500)
        }
        Metric("${template.sizeX()}x${template.sizeY()}x${template.sizeZ()}", I18n.get("structure:overlay.size"))
        Metric(template.totalBlocks().toString(), I18n.get("structure:overlay.blocks"))
        Metric(template.jigsaws().size.toString(), I18n.get("structure:overview.jigsaws"))
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.width(88.dp)
    ) {
        Text(value, style = StudioTypography.semiBold(13), color = StudioColors.Zinc200)
        Text(label, style = StudioTypography.regular(10), color = StudioColors.Zinc500)
    }
}

internal fun accentColor(seed: String): Color {
    val colors = listOf(
        StudioColors.Sky400,
        StudioColors.Emerald400,
        StudioColors.Amber400,
        StudioColors.Red400,
        StudioColors.Blue500,
        StudioColors.Violet500
    )
    return colors[kotlin.math.abs(seed.hashCode()) % colors.size]
}
