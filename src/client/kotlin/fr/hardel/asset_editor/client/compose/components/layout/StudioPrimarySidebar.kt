package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import kotlinx.coroutines.delay
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.lib.ChangesDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.compose.lib.rememberPermissions
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination
import fr.hardel.asset_editor.client.compose.lib.DebugDestination
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry
import net.minecraft.resources.Identifier

private val LOGO = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg")
private val DEBUG_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/debug.svg")
private val CHANGES_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/github.svg")
private val SETTINGS_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/settings.svg")

@Composable
fun StudioPrimarySidebar(context: StudioContext, modifier: Modifier = Modifier) {
    val permissions = rememberPermissions(context)
    val currentConcept = when (val destination = rememberCurrentDestination(context)) {
        is ConceptOverviewDestination -> destination.conceptId
        is ElementEditorDestination -> destination.conceptId
        else -> null
    }

    // aside: shrink-0 w-16 flex flex-col bg-sidebar
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(StudioColors.Sidebar)
    ) {
        // div: h-16 flex items-center justify-center
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable {
                    if (!permissions.isNone) {
                        StudioUiRegistry.firstSupportedConceptId()?.let { conceptId ->
                            context.navigationMemory().navigate(ConceptOverviewDestination(conceptId))
                        }
                    }
                }
        ) {
            SvgIcon(location = LOGO, size = 20.dp, tint = Color.White)
        }

        // div: overflow-y-auto overflow-x-hidden flex-1 flex flex-col items-center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            StudioUiRegistry.supportedConceptIds()
                .filter { !permissions.isNone }
                .forEachIndexed { index, conceptId ->
                    SidebarItemEnter(delayIndex = index) {
                        ConceptButton(
                            context = context,
                            conceptId = conceptId,
                            active = currentConcept == conceptId,
                            onClick = {
                                context.uiMemory().updateFilterPath(conceptId, "")
                                context.navigationMemory().navigate(ConceptOverviewDestination(conceptId))
                            }
                        )
                    }
                }
        }

        // div: shrink-0 flex flex-col-reverse items-center gap-2 mt-2
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            SidebarItemEnter(delayIndex = 0) {
                SidebarIconButton(icon = DEBUG_ICON) { context.navigationMemory().navigate(DebugDestination) }
            }
            SidebarItemEnter(delayIndex = 1) {
                SidebarIconButton(icon = CHANGES_ICON) { context.navigationMemory().navigate(ChangesDestination()) }
            }
            SidebarItemEnter(delayIndex = 2) {
                SidebarIconButton(icon = SETTINGS_ICON) {}
            }
        }
    }
}

@Composable
private fun SidebarItemEnter(delayIndex: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayIndex * 40L)
        progress.animateTo(1f, tween(StudioMotion.Medium2, easing = StudioMotion.EmphasizedDecelerate))
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationX = (1f - p) * -6.dp.toPx()
        }
    ) { content() }
}

@Composable
private fun ConceptButton(
    context: StudioContext,
    conceptId: Identifier,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            active -> StudioColors.Zinc700.copy(alpha = 0.05f)
            hovered -> StudioColors.Zinc800.copy(alpha = 0.25f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "concept-btn-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) StudioColors.Zinc800 else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "concept-btn-border"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
            .hoverable(interaction)
            .then(if (!active) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        ResourceImageIcon(
            context.studioIcon(conceptId),
            24.dp,
            modifier = Modifier.graphicsLayer { alpha = if (active) 1f else if (hovered) 0.9f else 0.8f }
        )
    }
}

@Composable
private fun SidebarIconButton(icon: Identifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val tint by animateColorAsState(
        targetValue = if (isHovered) Color.White else StudioColors.Zinc400,
        animationSpec = StudioMotion.hoverSpec(),
        label = "sidebar-icon-tint"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .graphicsLayer { alpha = if (isHovered) 1.0f else 0.7f }
    ) {
        SvgIcon(location = icon, size = 24.dp, tint = tint)
    }
}
