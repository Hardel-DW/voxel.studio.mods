package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.navigation.StudioEditorTab

data class StudioTabDefinition(
    val id: String,
    val translationKey: String,
    val tab: StudioEditorTab
)
