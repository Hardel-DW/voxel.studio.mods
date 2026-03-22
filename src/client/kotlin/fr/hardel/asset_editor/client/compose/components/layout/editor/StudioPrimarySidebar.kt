package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.compose.lib.rememberPermissions
import fr.hardel.asset_editor.client.navigation.DebugDestination
import net.minecraft.resources.Identifier

private val LOGO = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg")
private val DEBUG_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/debug.svg")
private val SETTINGS_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/settings.svg")

@Composable
fun StudioPrimarySidebar(context: StudioContext, modifier: Modifier = Modifier) {
    val permissions = rememberPermissions(context)
    val destination = rememberCurrentDestination(context)
    val currentConcept = when (destination) {
        is fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination -> destination.concept
        is fr.hardel.asset_editor.client.navigation.ConceptChangesDestination -> destination.concept
        is fr.hardel.asset_editor.client.navigation.ElementEditorDestination -> destination.concept
        else -> null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(VoxelColors.Sidebar)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable {
                    val overview = StudioConcept.firstAccessible(permissions)?.overview()
                        ?: StudioConcept.ENCHANTMENT.overview()
                    context.navigationState().navigate(overview)
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
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            StudioConcept.entries
                .filter { it != StudioConcept.STRUCTURE }
                .filter { !permissions.isNone }
                .forEach { concept ->
                    ConceptButton(
                        concept = concept,
                        active = currentConcept == concept,
                        onClick = {
                            context.uiState().updateFilterPath(concept, "")
                            context.navigationState().navigate(concept.overview())
                        }
                    )
                }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            SidebarIconButton(icon = DEBUG_ICON) { context.navigationState().navigate(DebugDestination) }
            SidebarIconButton(icon = SETTINGS_ICON) {}
        }
    }
}

@Composable
private fun ConceptButton(
    concept: StudioConcept,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .background(
                color = if (active) VoxelColors.ConceptActive else Color.Transparent,
                shape = shape
            )
            .then(
                if (active) Modifier.border(1.dp, VoxelColors.ConceptActiveBorder, shape)
                else Modifier
            )
            .then(if (!active) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .clickable(onClick = onClick)
    ) {
        ResourceImageIcon(concept.icon, 24.dp, modifier = Modifier.alpha(if (active) 1f else 0.8f))
    }
}

@Composable
private fun SidebarIconButton(icon: Identifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .alpha(if (isHovered) 1.0f else 0.7f)
    ) {
        SvgIcon(
            location = icon,
            size = 24.dp,
            tint = if (isHovered) Color.White else VoxelColors.Zinc400
        )
    }
}
