package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")
private val CODE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/diff/diff.svg")

@Composable
fun DiffEmptyState(selectedFile: String?, modifier: Modifier = Modifier) {
    val fallbackCodeIcon = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            selectedFile.isNullOrBlank() -> EmptyMessage(
                icon = SEARCH_ICON,
                title = I18n.get("changes:diff.select_file"),
                subtitle = null
            )
            !isPreviewable(selectedFile) -> EmptyMessage(
                icon = fallbackCodeIcon,
                title = I18n.get("changes:diff.preview_unavailable"),
                subtitle = selectedFile
            )
            else -> EmptyMessage(
                icon = CODE_ICON,
                title = I18n.get("changes:diff.no_changes"),
                subtitle = selectedFile
            )
        }
    }
}

@Composable
private fun EmptyMessage(icon: Identifier, title: String, subtitle: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.alpha(0.25f)) {
            SvgIcon(location = icon, size = 48.dp, tint = StudioColors.Zinc300)
        }
        Text(
            text = title,
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc500
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc600
            )
        }
    }
}

private fun isPreviewable(path: String): Boolean =
    path.endsWith(".json") || path.endsWith(".mcfunction") || path.endsWith(".mcmeta")
