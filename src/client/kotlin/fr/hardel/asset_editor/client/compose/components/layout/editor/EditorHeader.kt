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
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.tree.ConceptTreeState
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioElementId
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun EditorHeader(
    context: StudioContext,
    treeState: ConceptTreeState,
    concept: StudioConcept,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null
) {
    val destination = rememberCurrentDestination(context)
    val editorDestination = rememberCurrentElementDestination(context, concept)
    val isOverview = destination is ConceptOverviewDestination
    val segments = remember(destination, treeState.filterPath, treeState.selectedElementId, treeState.tree) {
        buildBreadcrumbSegments(treeState, concept, destination)
    }
    val title = remember(destination, segments, treeState.selectedElementId) {
        resolveTitle(treeState, concept, segments, destination)
    }
    val color = ColorUtils.accentColor(resolveColorKey(treeState, concept, destination))

    // header: relative shrink-0 overflow-hidden border-b border-zinc-800/50 bg-zinc-900/50
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VoxelColors.Zinc900.copy(alpha = 0.5f))
            .drawBehind {
                drawLine(
                    color = VoxelColors.Zinc800.copy(alpha = 0.5f),
                    start = Offset(0f, size.height - 0.5f),
                    end = Offset(size.width, size.height - 0.5f),
                    strokeWidth = 1f
                )
            }
    ) {
        // div: absolute/relative layers stack for tint, gradient and grid
        Box(modifier = Modifier.fillMaxWidth()) {
            // div.absolute.inset-0.mix-blend-overlay.opacity-40
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color.copy(alpha = 0.4f))
            )
            // div.absolute.inset-0.bg-linear-to-t from-zinc-950 via-zinc-950/80 to-transparent
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                VoxelColors.Zinc950.copy(alpha = 0.8f),
                                VoxelColors.Zinc950
                            )
                        )
                    )
            )
            // div.absolute.inset-0.bg-grid-white/[0.02] bg-size-[20px_20px]
            HeaderGrid(modifier = Modifier.matchParentSize())

            // div.relative.z-10.flex.flex-col.justify-end.p-8.pb-6
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 24.dp)
            ) {
                // div: flex items-end justify-between gap-8
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // div: space-y-2
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

                    if (isOverview && actions != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            actions()
                        }
                    }
                }

                if (editorDestination != null && concept.tabs.size > 1) {
                    // nav: flex items-center gap-1 mt-6 -mb-2
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
fun HeaderActionButton(
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // Link/button: px-4 py-2 bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 hover:text-zinc-200 text-zinc-400 rounded-lg text-sm
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(if (hovered) VoxelColors.Zinc800 else VoxelColors.Zinc900, RoundedCornerShape(8.dp))
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = VoxelColors.Zinc800,
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
    // div.absolute.inset-0.bg-grid-white/[0.02] bg-size-[20px_20px]
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
        return treeState.filterPath.ifBlank { "all" }
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
