package fr.hardel.asset_editor.client.navigation

import androidx.compose.runtime.Immutable

@Immutable
data class StudioTabEntry(
    val tabId: String,
    val destination: ElementEditorDestination
)
