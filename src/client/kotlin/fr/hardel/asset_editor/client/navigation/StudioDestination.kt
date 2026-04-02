package fr.hardel.asset_editor.client.navigation

import androidx.compose.runtime.Immutable
import net.minecraft.resources.Identifier

@Immutable
sealed interface StudioDestination

@Immutable
data object NoPermissionDestination : StudioDestination

@Immutable
data object DebugDestination : StudioDestination

@Immutable
data class ConceptOverviewDestination(
    val conceptId: Identifier
) : StudioDestination

@Immutable
data class ConceptChangesDestination(
    val conceptId: Identifier
) : StudioDestination

@Immutable
data class ConceptSimulationDestination(
    val conceptId: Identifier
) : StudioDestination

@Immutable
data class ElementEditorDestination(
    val conceptId: Identifier,
    val elementId: String,
    val tabId: Identifier
) : StudioDestination
