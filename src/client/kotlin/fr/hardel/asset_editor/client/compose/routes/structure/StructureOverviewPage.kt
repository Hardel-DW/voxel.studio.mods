package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.structure.STRUCTURE_CONCEPT_ID
import fr.hardel.asset_editor.client.compose.components.page.structure.rememberStructureTemplate
import fr.hardel.asset_editor.client.compose.components.ui.MetricColumn
import fr.hardel.asset_editor.client.compose.components.ui.OverviewListShell
import fr.hardel.asset_editor.client.compose.components.ui.SkeletonBox
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import fr.hardel.asset_editor.network.structure.StructureTemplateIndexEntry
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureOverviewPage(context: StudioContext, templates: List<StructureTemplateIndexEntry>) {
    val conceptId = STRUCTURE_CONCEPT_ID
    val conceptUi = rememberConceptUi(context, conceptId)

    OverviewListShell(
        title = I18n.get("structure:overview.title"),
        subtitle = I18n.get("structure:overview.subtitle", templates.size),
        searchPlaceholder = I18n.get("structure:overview.search"),
        entries = templates,
        search = conceptUi.search,
        onSearchChange = { context.uiMemory().updateSearch(conceptId, it) },
        matches = { template, query -> template.id().toString().contains(query, ignoreCase = true) },
        keyOf = { it.id().toString() }
    ) { template ->
        StructureTemplateRow(template) {
            context.navigationMemory().openElement(
                ElementEditorDestination(conceptId, template.id().toString(), context.studioDefaultEditorTab(conceptId))
            )
        }
    }
}

@Composable
private fun StructureTemplateRow(entry: StructureTemplateIndexEntry, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val template = rememberStructureTemplate(entry.id())
    val accent = remember(entry.id()) { ColorUtils.accentColor(entry.id().toString()) }
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
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.id().toString(), style = StudioTypography.semiBold(14), color = StudioColors.Zinc100)
            Text(entry.sourcePack(), style = StudioTypography.regular(11), color = StudioColors.Zinc500)
        }
        StructureTemplateMetrics(template)
    }
}

@Composable
private fun StructureTemplateMetrics(template: StructureTemplateSnapshot?) {
    if (template == null) {
        SkeletonMetric(I18n.get("structure:overlay.size"), Modifier.width(88.dp))
        SkeletonMetric(I18n.get("structure:overlay.blocks"), Modifier.width(88.dp))
        SkeletonMetric(I18n.get("structure:overview.jigsaws"), Modifier.width(88.dp))
        return
    }

    MetricColumn("${template.sizeX()}x${template.sizeY()}x${template.sizeZ()}", I18n.get("structure:overlay.size"), Modifier.width(88.dp))
    MetricColumn(template.totalBlocks().toString(), I18n.get("structure:overlay.blocks"), Modifier.width(88.dp))
    MetricColumn(template.jigsaws().size.toString(), I18n.get("structure:overview.jigsaws"), Modifier.width(88.dp))
}

@Composable
private fun SkeletonMetric(label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        SkeletonBox(width = 42.dp, height = 13.dp)
        Text(label, style = StudioTypography.regular(10), color = StudioColors.Zinc500)
    }
}
