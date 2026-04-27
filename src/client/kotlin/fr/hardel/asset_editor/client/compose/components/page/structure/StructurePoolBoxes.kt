package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.ui.geometry.Offset
import fr.hardel.asset_editor.client.compose.components.ui.scene.GridBounds
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DCamera
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DProjection
import fr.hardel.asset_editor.client.compose.components.ui.scene.Scene3DState
import fr.hardel.asset_editor.client.compose.components.ui.scene.SceneTrig
import fr.hardel.asset_editor.network.structure.StructurePieceBox
import androidx.compose.ui.unit.IntSize

fun pickPieceBoxAt(
    state: Scene3DState,
    boxes: List<StructurePieceBox>,
    bounds: GridBounds,
    point: Offset
): StructurePieceBox? {
    val viewport = state.viewport
    if (viewport.width <= 0 || viewport.height <= 0) return null
    val camera = state.frame?.camera ?: state.camera
    val trig = Scene3DProjection.trig(camera)

    var best: StructurePieceBox? = null
    var bestArea = Float.MAX_VALUE
    for (box in boxes) {
        val corners = projectCorners(box, bounds, camera, viewport, trig)
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (c in corners) {
            if (c.x < minX) minX = c.x
            if (c.x > maxX) maxX = c.x
            if (c.y < minY) minY = c.y
            if (c.y > maxY) maxY = c.y
        }
        if (point.x in minX..maxX && point.y in minY..maxY) {
            val area = (maxX - minX) * (maxY - minY)
            if (area < bestArea) {
                bestArea = area
                best = box
            }
        }
    }
    return best
}

private fun projectCorners(
    box: StructurePieceBox,
    bounds: GridBounds,
    camera: Scene3DCamera,
    viewport: IntSize,
    trig: SceneTrig
): Array<Offset> {
    val cx = bounds.sizeX * 0.5f
    val cy = bounds.sizeY * 0.5f
    val cz = bounds.sizeZ * 0.5f
    val x0 = box.minX().toFloat() - cx
    val x1 = (box.maxX() + 1).toFloat() - cx
    val y0 = box.minY().toFloat() - cy
    val y1 = (box.maxY() + 1).toFloat() - cy
    val z0 = box.minZ().toFloat() - cz
    val z1 = (box.maxZ() + 1).toFloat() - cz
    return arrayOf(
        Scene3DProjection.project(camera, viewport, trig, x0, y0, z0),
        Scene3DProjection.project(camera, viewport, trig, x1, y0, z0),
        Scene3DProjection.project(camera, viewport, trig, x1, y0, z1),
        Scene3DProjection.project(camera, viewport, trig, x0, y0, z1),
        Scene3DProjection.project(camera, viewport, trig, x0, y1, z0),
        Scene3DProjection.project(camera, viewport, trig, x1, y1, z0),
        Scene3DProjection.project(camera, viewport, trig, x1, y1, z1),
        Scene3DProjection.project(camera, viewport, trig, x0, y1, z1)
    )
}
