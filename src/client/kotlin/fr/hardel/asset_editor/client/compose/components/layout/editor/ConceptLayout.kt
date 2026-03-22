package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeController
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import net.minecraft.resources.Identifier

typealias SidebarExtra = @Composable () -> Unit
typealias PageFactory = @Composable (StudioRoute) -> Unit

class ConceptLayoutConfig(
    val concept: StudioConcept,
    val icon: Identifier,
    val sidebarTitleKey: String,
    val treeConfig: TreeController.Config,
    val simulationRoute: StudioRoute?,
    val showViewModeToggle: Boolean,
    val pageFactory: PageFactory,
    val sidebarExtras: List<SidebarExtra>
)

@Composable
fun ConceptLayout(
    context: StudioContext,
    config: ConceptLayoutConfig,
    modifier: Modifier = Modifier
) {
    val tree = remember(config.concept) {
        TreeController(context.router, config.treeConfig)
    }
    val route = context.router.currentRoute

    SideEffect {
        tree.setTree(config.treeConfig.tree)
        tree.setFolderIcons(config.treeConfig.folderIcons)
        tree.setDisableAutoExpand(config.treeConfig.disableAutoExpand)
    }

    LaunchedEffect(route, context.currentElementId) {
        if (route.concept() != config.concept.registry()) {
            return@LaunchedEffect
        }

        if (config.concept.tabRoutes().contains(route) && context.currentElementId.isBlank()) {
            context.tabsState().setCurrentElementId("")
            context.router.navigate(config.concept.overviewRoute)
            return@LaunchedEffect
        }

        if (route == config.concept.overviewRoute) {
            context.uiState().setSearch("")
        }
    }

    if (route.concept() != config.concept.registry()) {
        return
    }

    Row(
        modifier = modifier.fillMaxSize()
    ) {
        EditorSidebar(
            context = context,
            tree = tree,
            titleKey = config.sidebarTitleKey,
            iconPath = config.icon,
            topContent = config.sidebarExtras
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(VoxelColors.Editor)
        ) {
            EditorHeader(
                context = context,
                tree = tree,
                concept = config.concept,
                showViewToggle = config.showViewModeToggle,
                simulationRoute = config.simulationRoute
            )

            KeepAlivePages(
                currentRoute = route,
                concept = config.concept,
                pageFactory = config.pageFactory
            )
        }
    }
}

@Composable
private fun KeepAlivePages(
    currentRoute: StudioRoute,
    concept: StudioConcept,
    pageFactory: PageFactory
) {
    val allRoutes = remember(concept) {
        buildList {
            add(concept.overviewRoute)
            addAll(concept.tabRoutes())
        }
    }
    val visited = remember(concept) { mutableStateListOf<StudioRoute>() }

    if (currentRoute in allRoutes && currentRoute !in visited) {
        visited.add(currentRoute)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        visited.forEach { route ->
            val active = route == currentRoute
            key(route) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(if (!active) HiddenModifier else Modifier)
                ) {
                    pageFactory(route)
                }
            }
        }
    }
}

private val HiddenModifier = Modifier.drawWithContent { }
