package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ToggleGroup
import fr.hardel.asset_editor.client.compose.components.ui.ToggleOption
import fr.hardel.asset_editor.client.compose.components.ui.tree.ConceptTreeState
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioElementId
import fr.hardel.asset_editor.client.compose.lib.data.StudioViewMode
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioEditorTab
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val GRID_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/overview/grid.svg")
private val LIST_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/overview/list.svg")

@Composable
fun EditorHeader(
    context: StudioContext,
    treeState: ConceptTreeState,
    concept: StudioConcept,
    showViewToggle: Boolean,
    simulationTab: StudioEditorTab?,
    modifier: Modifier = Modifier
) {
    val destination = rememberCurrentDestination(context)
    val editorDestination = rememberCurrentElementDestination(context, concept)
    val conceptUi = rememberConceptUi(context, concept)
    val isOverview = destination is ConceptOverviewDestination
    val segments = remember(destination, treeState.filterPath, treeState.selectedElementId, treeState.tree) {
        buildBreadcrumbSegments(treeState, concept, destination)
    }
    val title = remember(destination, segments, treeState.selectedElementId) {
        resolveTitle(treeState, concept, segments, destination)
    }
    val color = ColorUtils.accentColor(resolveColorKey(treeState, concept, destination))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VoxelColors.SurfaceHeader)
            .drawBehind {
                drawLine(
                    color = VoxelColors.BorderAlpha50,
                    start = Offset(0f, size.height - 0.5f),
                    end = Offset(size.width, size.height - 0.5f),
                    strokeWidth = 1f
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, VoxelColors.SurfaceOverlay, VoxelColors.SurfaceOverlay)
                        )
                    )
            )
            HeaderGrid(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        EditorBreadcrumb(
                            rootLabel = I18n.get(concept.titleKey),
                            segments = segments,
                            showBack = !isOverview,
                            onBack = { context.navigationState().navigate(concept.overview()) }
                        )

                        Text(
                            text = title,
                            style = VoxelTypography.minecraftTen(36),
                            color = Color.White
                        )

                        Box(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .width(96.dp)
                                .height(1.dp)
                                .background(Brush.horizontalGradient(listOf(color, Color.Transparent)))
                        )
                    }

                    if (isOverview) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (simulationTab != null && editorDestination != null) {
                                HeaderActionButton(
                                    text = I18n.get("enchantment:simulation"),
                                    onClick = {
                                        context.navigationState().replaceCurrentTab(
                                            editorDestination.copy(tab = simulationTab)
                                        )
                                    }
                                )
                            }

                            if (showViewToggle) {
                                ToggleGroup(
                                    options = listOf(
                                        ToggleOption.IconOption("grid", GRID_ICON),
                                        ToggleOption.IconOption("list", LIST_ICON)
                                    ),
                                    selectedValue = conceptUi.viewMode.name.lowercase(),
                                    onValueChange = { value ->
                                        context.uiState().updateViewMode(
                                            concept,
                                            if (value == "list") StudioViewMode.LIST else StudioViewMode.GRID
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (editorDestination != null && concept.tabs.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .offset(y = 8.dp)
                    ) {
                        concept.tabs.forEach { tab ->
                            EditorHeaderTabItem(
                                label = I18n.get(tab.translationKey),
                                active = tab.tab == editorDestination.tab,
                                onClick = {
                                    context.navigationState().replaceCurrentTab(
                                        editorDestination.copy(tab = tab.tab)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(if (hovered) VoxelColors.Zinc800 else VoxelColors.SurfaceRaised, RoundedCornerShape(8.dp))
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = VoxelColors.Border,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
            }
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = VoxelTypography.medium(13),
            color = if (hovered) VoxelColors.Zinc300 else VoxelColors.Zinc400
        )
    }
}

@Composable
private fun HeaderGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val step = 20.dp.toPx()

        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = Color.White.copy(alpha = 0.02f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }

        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.02f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
    }
}

private fun resolveTitle(
    treeState: ConceptTreeState,
    concept: StudioConcept,
    segments: List<String>,
    destination: fr.hardel.asset_editor.client.navigation.StudioDestination
): String {
    if (destination is ConceptOverviewDestination) {
        return segments.lastOrNull() ?: I18n.get("generic:all")
    }

    val id = treeState.selectedElementId
    if (id.isNullOrBlank()) {
        return I18n.get(concept.titleKey)
    }

    val parsed = StudioElementId.parse(id) ?: return id
    return StudioText.resolve(concept.registryKey, parsed.identifier)
}

private fun resolveColorKey(
    treeState: ConceptTreeState,
    concept: StudioConcept,
    destination: fr.hardel.asset_editor.client.navigation.StudioDestination
): String {
    if (destination is ConceptOverviewDestination) {
        return if (treeState.filterPath.isBlank()) "all" else treeState.filterPath
    }
    return treeState.selectedElementId ?: concept.registry()
}

private fun buildBreadcrumbSegments(
    treeState: ConceptTreeState,
    concept: StudioConcept,
    destination: fr.hardel.asset_editor.client.navigation.StudioDestination
): List<String> {
    if (destination is ConceptOverviewDestination) {
        return resolveFilterPathLabels(treeState)
    }

    val id = treeState.selectedElementId ?: return emptyList()
    val parsed = StudioElementId.parse(id) ?: return listOf(id)
    val resourceParts = parsed.resourcePath().split("/")

    return buildList {
        add(parsed.namespace())
        resourceParts.forEachIndexed { index, part ->
            val segmentId = if (index == resourceParts.lastIndex) {
                parsed.identifier
            } else {
                Identifier.fromNamespaceAndPath(parsed.namespace(), part)
            }
            add(StudioText.resolve(concept.registryKey, segmentId))
        }
    }
}

private fun resolveFilterPathLabels(treeState: ConceptTreeState): List<String> {
    val filterPath = treeState.filterPath
    if (filterPath.isBlank()) {
        return emptyList()
    }

    val parts = filterPath.split("/")
    val labels = ArrayList<String>(parts.size)
    var cursor: TreeNodeModel? = treeState.tree
    parts.forEach { part ->
        val child = cursor?.children?.get(part)
        if (child == null) {
            labels += part
            cursor = null
        } else {
            labels += child.label?.takeUnless { it.isBlank() } ?: part
            cursor = child
        }
    }
    return labels
}
