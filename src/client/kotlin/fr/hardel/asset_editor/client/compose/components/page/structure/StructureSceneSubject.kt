package fr.hardel.asset_editor.client.compose.components.page.structure

import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import net.minecraft.resources.Identifier

sealed interface StructureSceneSubject {
    val id: Identifier
    val sizeX: Int
    val sizeY: Int
    val sizeZ: Int
    val stageCount: Int

    fun forEachVoxel(block: (blockId: Identifier, blockStateId: Int, x: Int, y: Int, z: Int, stage: Int, finalStateId: Int) -> Unit)

    data class Template(val template: StructureTemplateSnapshot) : StructureSceneSubject {
        override val id get() = template.id()
        override val sizeX get() = template.sizeX()
        override val sizeY get() = template.sizeY()
        override val sizeZ get() = template.sizeZ()
        override val stageCount get() = 0
        override fun forEachVoxel(block: (Identifier, Int, Int, Int, Int, Int, Int) -> Unit) {
            for (v in template.voxels()) block(v.blockId(), v.blockStateId(), v.x(), v.y(), v.z(), 0, v.finalStateId())
        }
    }

    data class Assembly(val assembly: StructureAssemblySnapshot) : StructureSceneSubject {
        override val id get() = assembly.id()
        override val sizeX get() = assembly.sizeX()
        override val sizeY get() = assembly.sizeY()
        override val sizeZ get() = assembly.sizeZ()
        override val stageCount get() = assembly.pieceCount()
        override fun forEachVoxel(block: (Identifier, Int, Int, Int, Int, Int, Int) -> Unit) {
            for (v in assembly.voxels()) block(v.blockId(), v.blockStateId(), v.x(), v.y(), v.z(), v.pieceIndex(), v.finalStateId())
        }
    }
}
