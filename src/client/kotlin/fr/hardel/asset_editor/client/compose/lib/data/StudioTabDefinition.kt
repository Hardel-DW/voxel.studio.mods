package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.compose.routes.StudioRoute

data class StudioTabDefinition(
    val id: String,
    val translationKey: String,
    val route: StudioRoute
)
