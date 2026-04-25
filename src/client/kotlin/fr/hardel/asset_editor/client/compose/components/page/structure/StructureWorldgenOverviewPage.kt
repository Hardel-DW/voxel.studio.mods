package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val JIGSAW_FALLBACK = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/jigsaw.svg")

@Composable
fun StructureWorldgenOverviewPage(
    context: StudioContext,
    entries: List<StructureWorldgenSnapshot>
) {
    val conceptId = STRUCTURE_CONCEPT_ID
    val conceptUi = rememberConceptUi(context, conceptId)
    val search = conceptUi.search
    val visible = remember(entries, search) {
        entries.filter { search.isBlank() || it.id().toString().contains(search, ignoreCase = true) }
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
                Text(I18n.get("structure:worldgen.title"), style = StudioTypography.semiBold(28), color = StudioColors.Zinc100)
                Text(I18n.get("structure:worldgen.subtitle", entries.size), style = StudioTypography.regular(13), color = StudioColors.Zinc500)
            }
            InputText(
                value = search,
                onValueChange = { context.uiMemory().updateSearch(conceptId, it) },
                placeholder = I18n.get("structure:worldgen.search"),
                maxWidth = 360.dp,
                focusExpand = false
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visible, key = { it.id().toString() }) { entry ->
                StructureWorldgenRow(
                    context = context,
                    entry = entry,
                    onClick = {
                        context.navigationMemory().openElement(
                            ElementEditorDestination(conceptId, entry.id().toString(), context.studioDefaultEditorTab(conceptId))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun StructureWorldgenRow(
    context: StudioContext,
    entry: StructureWorldgenSnapshot,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val customIcon = remember(entry.id()) {
        Identifier.fromNamespaceAndPath(entry.id().namespace, "icons/structure/${entry.id().path}.svg")
    }
    val resolvedIcon = if (context.assetCache().svg(customIcon) != null) customIcon else JIGSAW_FALLBACK

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
        Box(resolvedIcon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.id().toString(), style = StudioTypography.semiBold(14), color = StudioColors.Zinc100)
            Text(entry.sourcePack(), style = StudioTypography.regular(11), color = StudioColors.Zinc500)
        }
        WorldgenMetric(entry.type().substringAfterLast(':').ifEmpty { "?" }, I18n.get("structure:worldgen.type"))
        WorldgenMetric(entry.size().toString(), I18n.get("structure:worldgen.size"))
    }
}

@Composable
private fun Box(icon: Identifier) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StudioColors.Zinc950)
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(6.dp))
    ) {
        SvgIcon(icon, 22.dp, StudioColors.Zinc200)
    }
}

@Composable
private fun WorldgenMetric(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.width(96.dp)
    ) {
        Text(value, style = StudioTypography.semiBold(13), color = StudioColors.Zinc200)
        Text(label, style = StudioTypography.regular(10), color = StudioColors.Zinc500)
    }
}
