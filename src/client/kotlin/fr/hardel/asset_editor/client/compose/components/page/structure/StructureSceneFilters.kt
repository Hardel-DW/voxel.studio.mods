package fr.hardel.asset_editor.client.compose.components.page.structure

data class StructureSceneFilters(
    val displayedStage: Int = Int.MAX_VALUE,
    val animatingStage: Int = -1,
    val sliceY: Int = Int.MAX_VALUE,
    val showJigsaws: Boolean = true,
    val highlight: String? = null,
    val drop: Float = 0f
)
