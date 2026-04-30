package fr.hardel.asset_editor.client.compose.components.page.structure

data class StructureSceneFilters(
    val displayedStage: Int = Int.MAX_VALUE,
    val sliceY: Int = Int.MAX_VALUE,
    val showJigsaws: Boolean = true,
    val highlight: String? = null
)
