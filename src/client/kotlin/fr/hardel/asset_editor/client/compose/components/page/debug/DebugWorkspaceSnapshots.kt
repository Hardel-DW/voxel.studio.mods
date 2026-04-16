package fr.hardel.asset_editor.client.compose.components.page.debug

data class DebugWorkspaceOverview(
    val destination: String,
    val selectedPack: String,
    val currentElement: String,
    val pendingActions: Int,
    val openTabs: Int,
    val registries: Int,
    val totalEntries: Int
)

data class DebugWorkspaceRegistriesSnapshot(val counts: Map<String, Int>)
