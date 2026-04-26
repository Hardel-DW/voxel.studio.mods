package fr.hardel.asset_editor.client.compose.components.page.structure

/**
 * Render-time filters applied to a [StructureSceneSubject].
 *
 * @param displayedStage     Voxels with stage < this are kept. [Int.MAX_VALUE] means "all".
 * @param animatingStage     Voxels with stage == this go into the animating mesh
 *                           (drawn with [drop] applied at draw time). -1 = no animating piece.
 * @param sliceY             Voxels with y > this are clipped.
 * @param showJigsaws        When false, vanilla `minecraft:jigsaw` blocks are removed.
 * @param highlight          Optional substring; matching block ids are drawn slightly enlarged.
 * @param drop               Vertical offset (model-space) applied to the animating mesh only.
 */
data class StructureSceneFilters(
    val displayedStage: Int = Int.MAX_VALUE,
    val animatingStage: Int = -1,
    val sliceY: Int = Int.MAX_VALUE,
    val showJigsaws: Boolean = true,
    val highlight: String? = null,
    val drop: Float = 0f
)
