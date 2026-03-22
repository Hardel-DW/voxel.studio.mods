package fr.hardel.asset_editor.client.navigation

import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import kotlinx.collections.immutable.ImmutableSet

interface StudioConceptSpec {
    val concept: StudioConcept
    val defaultEditorTab: StudioEditorTab
    val supportedTabs: ImmutableSet<StudioEditorTab>

    fun overview(): ConceptOverviewDestination

    fun changes(): ConceptChangesDestination

    fun editor(
        elementId: String,
        tab: StudioEditorTab = defaultEditorTab
    ): ElementEditorDestination
}
