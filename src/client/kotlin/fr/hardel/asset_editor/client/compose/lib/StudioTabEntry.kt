package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Immutable

@Immutable
data class StudioTabEntry(
    val tabId: String,
    val destination: ElementEditorDestination
)