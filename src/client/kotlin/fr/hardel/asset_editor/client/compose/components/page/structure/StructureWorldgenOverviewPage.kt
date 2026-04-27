package fr.hardel.asset_editor.client.compose.components.page.structure

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import fr.hardel.asset_editor.client.compose.components.ui.MetricColumn
import fr.hardel.asset_editor.client.compose.components.ui.OverviewListShell
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun StructureWorldgenOverviewPage(
    context: StudioContext,
    entries: List<StructureWorldgenSnapshot>
) {
    val conceptId = STRUCTURE_CONCEPT_ID
    val conceptUi = rememberConceptUi(context, conceptId)

    OverviewListShell(
        title = I18n.get("structure:worldgen.title"),
        subtitle = I18n.get("structure:worldgen.subtitle", entries.size),
        searchPlaceholder = I18n.get("structure:worldgen.search"),
        entries = entries,
        search = conceptUi.search,
        onSearchChange = { context.uiMemory().updateSearch(conceptId, it) },
        matches = { entry, query -> entry.id().toString().contains(query, ignoreCase = true) },
        keyOf = { it.id().toString() }
    ) { entry ->
        WorldgenRow(context, entry) {
            context.navigationMemory().openElement(
                ElementEditorDestination(conceptId, entry.id().toString(), context.studioDefaultEditorTab(conceptId))
            )
        }
    }
}

@Composable
private fun WorldgenRow(
    context: StudioContext,
    entry: StructureWorldgenSnapshot,
    onClick: () -> Unit
) {
    val supported = entry.previewSupported()
    val palette = if (supported) RowPalette.SUPPORTED else RowPalette.UNSUPPORTED
    val shape = RoundedCornerShape(8.dp)
    val resolvedIcon = remember(entry.id()) { resolveStructureIcon(entry.id(), context.assetCache()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(shape)
            .background(StudioColors.Zinc900.copy(alpha = palette.backgroundAlpha))
            .border(1.dp, StudioColors.Zinc800, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(resolvedIcon, palette.iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.id().toString(), style = StudioTypography.semiBold(14), color = palette.title)
            Text(entry.sourcePack(), style = StudioTypography.regular(11), color = palette.subtitle)
        }
        if (!supported) {
            UnsupportedBadge()
        }

        MetricColumn(
            value = entry.type().substringAfterLast(':').ifEmpty { "?" },
            label = I18n.get("structure:worldgen.type"),
            modifier = Modifier.width(96.dp)
        )
        
        MetricColumn(
            value = entry.size().toString(),
            label = I18n.get("structure:worldgen.size"),
            modifier = Modifier.width(96.dp)
        )
    }
}

@Composable
private fun IconBadge(icon: Identifier, tint: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StudioColors.Zinc950)
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(6.dp))
    ) {
        SvgIcon(icon, 22.dp, tint)
    }
}

@Composable
private fun UnsupportedBadge() {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(StudioColors.Zinc800.copy(alpha = 0.6f))
            .border(1.dp, StudioColors.Zinc700, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = I18n.get("structure:worldgen.preview_unavailable"),
            style = StudioTypography.regular(10),
            color = StudioColors.Zinc400
        )
    }
}

private enum class RowPalette(
    val backgroundAlpha: Float,
    val title: Color,
    val subtitle: Color,
    val iconTint: Color
) {
    SUPPORTED(0.55f, StudioColors.Zinc100, StudioColors.Zinc500, StudioColors.Zinc200),
    UNSUPPORTED(0.32f, StudioColors.Zinc500, StudioColors.Zinc600, StudioColors.Zinc600)
}
