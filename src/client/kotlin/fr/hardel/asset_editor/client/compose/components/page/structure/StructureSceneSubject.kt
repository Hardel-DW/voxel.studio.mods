package fr.hardel.asset_editor.client.compose.components.page.structure

import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import net.minecraft.resources.Identifier

/**
 * Geometry source feeding [StructureSceneArea].
 *
 * The structure viewer draws two distinct payloads with the same shader / camera pipeline:
 *  - [Template]  — a single .nbt template (PIECES tab). All voxels are static, no pagination.
 *  - [Assembly]  — a multi-piece worldgen assembly (STRUCTURE tab). Each voxel carries a
 *                  pieceIndex and the user paginates through them.
 *
 * Exposing them through one interface lets the rest of the rendering stack be generic.
 */
sealed interface StructureSceneSubject {
    val id: Identifier
    val sizeX: Int
    val sizeY: Int
    val sizeZ: Int

    /** Number of pagination stages. 0 means the subject is single-stage (no pagination). */
    val stageCount: Int

    /** Iterates voxels with their stage index (0 for templates). [blockStateId] indexes [Block.BLOCK_STATE_REGISTRY]. */
    fun forEachVoxel(block: (blockId: Identifier, blockStateId: Int, x: Int, y: Int, z: Int, stage: Int) -> Unit)

    data class Template(val template: StructureTemplateSnapshot) : StructureSceneSubject {
        override val id get() = template.id()
        override val sizeX get() = template.sizeX()
        override val sizeY get() = template.sizeY()
        override val sizeZ get() = template.sizeZ()
        override val stageCount get() = 0
        override fun forEachVoxel(block: (Identifier, Int, Int, Int, Int, Int) -> Unit) {
            for (v in template.voxels()) block(v.blockId(), v.blockStateId(), v.x(), v.y(), v.z(), 0)
        }
    }

    data class Assembly(val assembly: StructureAssemblySnapshot) : StructureSceneSubject {
        override val id get() = assembly.id()
        override val sizeX get() = assembly.sizeX()
        override val sizeY get() = assembly.sizeY()
        override val sizeZ get() = assembly.sizeZ()
        override val stageCount get() = assembly.pieceCount()
        override fun forEachVoxel(block: (Identifier, Int, Int, Int, Int, Int) -> Unit) {
            for (v in assembly.voxels()) block(v.blockId(), v.blockStateId(), v.x(), v.y(), v.z(), v.pieceIndex())
        }
    }
}
