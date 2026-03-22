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
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeController
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioElementId
import fr.hardel.asset_editor.client.compose.lib.data.StudioViewMode
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val GRID_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/overview/grid.svg")
private val LIST_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/overview/list.svg")

@Composable
fun EditorHeader(
    context: StudioContext,
    tree: TreeController,
    concept: StudioConcept,
    showViewToggle: Boolean,
    simulationRoute: StudioRoute?,
    modifier: Modifier = Modifier
) {
    val route = context.router.currentRoute
    val segments = remember(route, context.filterPath, context.currentElementId, tree.tree) {
        buildBreadcrumbSegments(tree, concept, route)
    }
    val title = remember(route, segments, context.currentElementId) {
        resolveTitle(tree, concept, segments, route)
    }
    val color = ColorUtils.accentColor(resolveColorKey(tree, concept, route))

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
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
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
            HeaderGrid(
                modifier = Modifier.matchParentSize()
            )

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
                            showBack = !route.isOverview(),
                            onBack = { context.router.navigate(concept.overviewRoute) }
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

                    if (route.isOverview()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (simulationRoute != null) {
                                HeaderActionButton(
                                    text = I18n.get("enchantment:simulation"),
                                    onClick = { context.router.navigate(simulationRoute) }
                                )
                            }

                            if (showViewToggle) {
                                ToggleGroup(
                                    options = listOf(
                                        ToggleOption.IconOption("grid", GRID_ICON),
                                        ToggleOption.IconOption("list", LIST_ICON)
                                    ),
                                    selectedValue = context.viewMode.name.lowercase(),
                                    onValueChange = { value ->
                                        context.updateViewMode(
                                            if (value == "list") StudioViewMode.LIST else StudioViewMode.GRID
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (showTabs(tree, concept, route)) {
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
                                active = route == tab.route,
                                onClick = { context.router.navigate(tab.route) }
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

private fun showTabs(
    tree: TreeController,
    concept: StudioConcept,
    route: StudioRoute
): Boolean {
    if (concept.tabs.size <= 1) {
        return false
    }
    if (tree.currentElementId().isNullOrBlank()) {
        return false
    }
    return route.concept() == concept.registry() && concept.tabRoutes().contains(route)
}

private fun resolveTitle(
    tree: TreeController,
    concept: StudioConcept,
    segments: List<String>,
    route: StudioRoute
): String {
    if (route.isOverview()) {
        return segments.lastOrNull() ?: I18n.get("generic:all")
    }

    val id = tree.currentElementId()
    if (id.isNullOrBlank()) {
        return I18n.get(concept.titleKey)
    }

    val parsed = StudioElementId.parse(id) ?: return id
    return StudioText.resolve(concept.registryKey, parsed.identifier)
}

private fun resolveColorKey(
    tree: TreeController,
    concept: StudioConcept,
    route: StudioRoute
): String {
    if (route.isOverview()) {
        val filterPath = tree.filterPath()
        return if (filterPath.isBlank()) "all" else filterPath
    }
    return tree.currentElementId().takeUnless { it.isNullOrBlank() } ?: concept.registry()
}

private fun buildBreadcrumbSegments(
    tree: TreeController,
    concept: StudioConcept,
    route: StudioRoute
): List<String> {
    if (route.isOverview()) {
        return resolveFilterPathLabels(tree)
    }

    val id = tree.currentElementId() ?: return emptyList()
    val parsed = StudioElementId.parse(id) ?: return listOf(id)

    val segments = mutableListOf(parsed.namespace())
    parsed.resourcePath().split("/").forEachIndexed { index, part ->
        val segmentId = if (index == parsed.resourcePath().split("/").lastIndex) {
            parsed.identifier
        } else {
            Identifier.fromNamespaceAndPath(parsed.namespace(), part)
        }
        segments += StudioText.resolve(concept.registryKey, segmentId)
    }
    return segments
}

private fun resolveFilterPathLabels(tree: TreeController): List<String> {
    val filterPath = tree.filterPath()
    if (filterPath.isBlank()) {
        return emptyList()
    }

    val parts = filterPath.split("/")
    val labels = ArrayList<String>(parts.size)
    var cursor: TreeNodeModel? = tree.tree
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
