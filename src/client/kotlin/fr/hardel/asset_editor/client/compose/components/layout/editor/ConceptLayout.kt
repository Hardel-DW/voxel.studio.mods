package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.components.ui.tree.ConceptTreeState
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.navigation.StudioEditorTab
import net.minecraft.resources.Identifier

typealias SidebarExtra = @Composable () -> Unit
typealias DestinationPageFactory = @Composable (StudioDestination) -> Unit

class ConceptLayoutConfig(
    val concept: StudioConcept,
    val icon: Identifier,
    val sidebarTitleKey: String,
    val treeState: ConceptTreeState,
    val simulationTab: StudioEditorTab?,
    val pageFactory: DestinationPageFactory,
    val sidebarExtras: List<SidebarExtra>
)

@Composable
fun ConceptLayout(
    context: StudioContext,
    config: ConceptLayoutConfig,
    modifier: Modifier = Modifier
) {
    val destination = rememberCurrentDestination(context)
    val destinationConcept = destination.conceptOrNull()
    if (destinationConcept != config.concept) {
        return
    }

    LaunchedEffect(destination) {
        context.prefetcher().prefetch(destination)
    }

    // div: flex size-full overflow-hidden relative z-10 isolate
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(VoxelColors.Zinc950)
    ) {
        // aside: EditorSidebar equivalent
        EditorSidebar(
            context = context,
            treeState = config.treeState,
            titleKey = config.sidebarTitleKey,
            iconPath = config.icon,
            topContent = config.sidebarExtras
        )

        // main: flex-1 flex flex-col min-w-0 relative bg-zinc-950
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(VoxelColors.Zinc950)
        ) {
            EditorHeader(
                context = context,
                treeState = config.treeState,
                concept = config.concept,
                simulationTab = config.simulationTab
            )

            config.pageFactory(destination)
        }
    }
}

private fun StudioDestination.conceptOrNull(): StudioConcept? =
    when (this) {
        is ConceptOverviewDestination -> concept
        is ConceptChangesDestination -> concept
        is ElementEditorDestination -> concept
        else -> null
    }
