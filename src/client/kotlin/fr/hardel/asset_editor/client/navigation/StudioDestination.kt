package fr.hardel.asset_editor.client.navigation

import androidx.compose.runtime.Immutable
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept

@Immutable
sealed interface StudioDestination

@Immutable
data object NoPermissionDestination : StudioDestination

@Immutable
data object DebugDestination : StudioDestination

@Immutable
data class ConceptOverviewDestination(
    val concept: StudioConcept
) : StudioDestination

@Immutable
data class ConceptChangesDestination(
    val concept: StudioConcept
) : StudioDestination

@Immutable
data class ElementEditorDestination(
    val concept: StudioConcept,
    val elementId: String,
    val tab: StudioEditorTab
) : StudioDestination
