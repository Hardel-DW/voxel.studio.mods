package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.StudioRouter
import net.minecraft.resources.Identifier

private val LOGO = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg")
private val DEBUG_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/debug.svg")
private val SETTINGS_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/settings.svg")

@Composable
fun StudioPrimarySidebar(router: StudioRouter, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(64.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable {
                    val overview = StudioRoute.overviewOf(router.currentRoute.concept)
                    router.navigate(overview)
                }
        ) {
            SvgIcon(location = LOGO, size = 20.dp, tint = Color.White)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Concept cards will go here when concepts are wired
        }

        Spacer(Modifier.height(8.dp))

        SidebarIconButton(icon = DEBUG_ICON) { router.navigate(StudioRoute.Debug) }
        SidebarIconButton(icon = SETTINGS_ICON) {}

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SidebarIconButton(icon: Identifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .alpha(if (isHovered) 1.0f else 0.7f)
    ) {
        SvgIcon(location = icon, size = 24.dp, tint = Color.White)
    }
}
