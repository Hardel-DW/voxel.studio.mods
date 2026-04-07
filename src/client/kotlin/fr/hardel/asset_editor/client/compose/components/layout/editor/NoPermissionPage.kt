package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val LOCK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/lock.svg")

@Composable
fun NoPermissionPage(modifier: Modifier = Modifier) {
    // Compose-only: état vide d'autorisation, sans équivalent TSX direct dans le layout editor.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        // div: flex flex-col items-center gap-4
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.alpha(0.5f)) {
                SvgIcon(location = LOCK_ICON, size = 48.dp, tint = StudioColors.Zinc500)
            }

            Text(
                text = I18n.get("studio:permission.no_access.title"),
                style = StudioTypography.bold(20),
                color = StudioColors.Zinc400
            )

            Text(
                text = I18n.get("studio:permission.no_access.description"),
                style = StudioTypography.regular(14),
                color = StudioColors.Zinc600,
                textAlign = TextAlign.Center
            )
        }
    }
}
