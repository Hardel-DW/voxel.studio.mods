package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.hardel.asset_editor.client.rendering.StructureSceneTessellator.CompiledLayer;

final class StructureSceneLineTessellator {

    private static final int BUFFER_INITIAL_CAPACITY = 32768;
    private static final float LINE_WIDTH = 1.5f;
    private static final int COLOR_AMBER = 0xFFFBBF24;

    static Map<Integer, CompiledLayer> tessellate(List<StructureSceneRenderer.PieceBox> boxes) {
        if (boxes.isEmpty())
            return Map.of();
        Map<Integer, List<StructureSceneRenderer.PieceBox>> byPiece = groupByPiece(boxes);
        Map<Integer, CompiledLayer> out = new HashMap<>(byPiece.size());
        for (Map.Entry<Integer, List<StructureSceneRenderer.PieceBox>> entry : byPiece.entrySet()) {
            CompiledLayer layer = buildLayer(entry.getValue());
            if (layer != null)
                out.put(entry.getKey(), layer);
        }
        return out;
    }

    private static Map<Integer, List<StructureSceneRenderer.PieceBox>> groupByPiece(List<StructureSceneRenderer.PieceBox> boxes) {
        Map<Integer, List<StructureSceneRenderer.PieceBox>> out = new HashMap<>();
        for (StructureSceneRenderer.PieceBox b : boxes) {
            out.computeIfAbsent(b.pieceIndex(), k -> new ArrayList<>()).add(b);
        }
        return out;
    }

    private static CompiledLayer buildLayer(List<StructureSceneRenderer.PieceBox> boxes) {
        ByteBufferBuilder bb = new ByteBufferBuilder(BUFFER_INITIAL_CAPACITY);
        try {
            BufferBuilder builder = new BufferBuilder(bb, VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
            PoseStack pose = new PoseStack();
            for (StructureSceneRenderer.PieceBox box : boxes)
                emitBoxEdges(builder, pose, box);
            MeshData mesh = builder.build();
            if (mesh == null) {
                bb.close();
                return null;
            }
            ByteBuffer src = mesh.vertexBuffer();
            ByteBuffer cached = MemoryUtil.memAlloc(src.remaining());
            cached.put(src.duplicate());
            cached.flip();
            MeshData.DrawState drawState = mesh.drawState();
            mesh.close();
            bb.close();
            return new CompiledLayer(RenderTypes.LINES, cached, drawState);
        } catch (RuntimeException e) {
            bb.close();
            throw e;
        }
    }

    private static void emitBoxEdges(VertexConsumer builder, PoseStack poseStack, StructureSceneRenderer.PieceBox box) {
        float x0 = box.minX();
        float y0 = box.minY();
        float z0 = box.minZ();
        float x1 = box.maxX() + 1f;
        float y1 = box.maxY() + 1f;
        float z1 = box.maxZ() + 1f;
        edge(builder, poseStack, x0, y0, z0, x1, y0, z0);
        edge(builder, poseStack, x1, y0, z0, x1, y0, z1);
        edge(builder, poseStack, x1, y0, z1, x0, y0, z1);
        edge(builder, poseStack, x0, y0, z1, x0, y0, z0);
        edge(builder, poseStack, x0, y1, z0, x1, y1, z0);
        edge(builder, poseStack, x1, y1, z0, x1, y1, z1);
        edge(builder, poseStack, x1, y1, z1, x0, y1, z1);
        edge(builder, poseStack, x0, y1, z1, x0, y1, z0);
        edge(builder, poseStack, x0, y0, z0, x0, y1, z0);
        edge(builder, poseStack, x1, y0, z0, x1, y1, z0);
        edge(builder, poseStack, x1, y0, z1, x1, y1, z1);
        edge(builder, poseStack, x0, y0, z1, x0, y1, z1);
    }

    private static void edge(VertexConsumer builder, PoseStack poseStack, float x1, float y1, float z1, float x2, float y2, float z2) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize();
        PoseStack.Pose pose = poseStack.last();
        builder.addVertex(pose, x1, y1, z1).setColor(COLOR_AMBER).setNormal(pose, normal).setLineWidth(LINE_WIDTH);
        builder.addVertex(pose, x2, y2, z2).setColor(COLOR_AMBER).setNormal(pose, normal).setLineWidth(LINE_WIDTH);
    }

    private StructureSceneLineTessellator() {}
}
