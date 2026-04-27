package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CPU mesh build per (pieceIndex, y) cell: {@code stack} = full occlusion, {@code expose} = truncated for slice reveal. */
final class StructureSceneTessellator {

    static final StaticMesh EMPTY_STATIC = new StaticMesh(Map.of(), Map.of());

    static StaticMesh tessellateStatic(StructureSceneRenderer.Request request) {
        Map<Cell, List<StructureSceneRenderer.Voxel>> grouped = groupByPieceAndY(request.voxels());
        if (grouped.isEmpty()) return EMPTY_STATIC;

        StructureBlockView fullLevel = new StructureBlockView(request.voxels(), request.sizeY(), Integer.MAX_VALUE);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        List<BlockModelPart> parts = new ArrayList<>();

        Map<Cell, List<CompiledLayer>> stack = new HashMap<>();
        Map<Cell, List<CompiledLayer>> expose = new HashMap<>();
        Map<Integer, StructureBlockView> truncatedByY = new HashMap<>();

        for (Map.Entry<Cell, List<StructureSceneRenderer.Voxel>> entry : grouped.entrySet()) {
            Cell cell = entry.getKey();
            List<StructureSceneRenderer.Voxel> bucket = entry.getValue();
            stack.put(cell, tessellateLayer(bucket, fullLevel, dispatcher, random, parts));
            BlockAndTintGetter truncated = truncatedByY.computeIfAbsent(cell.y,
                y -> new StructureBlockView(request.voxels(), request.sizeY(), y));
            expose.put(cell, tessellateLayer(bucket, truncated, dispatcher, random, parts));
        }

        return new StaticMesh(stack, expose);
    }

    private static Map<Cell, List<StructureSceneRenderer.Voxel>> groupByPieceAndY(List<StructureSceneRenderer.Voxel> voxels) {
        Map<Cell, List<StructureSceneRenderer.Voxel>> grouped = new HashMap<>();
        for (StructureSceneRenderer.Voxel v : voxels) {
            grouped.computeIfAbsent(new Cell(v.pieceIndex(), v.y()), k -> new ArrayList<>()).add(v);
        }
        return grouped;
    }

    private static List<CompiledLayer> tessellateLayer(
        List<StructureSceneRenderer.Voxel> voxels,
        BlockAndTintGetter level,
        BlockRenderDispatcher dispatcher,
        RandomSource random,
        List<BlockModelPart> partsScratch
    ) {
        Map<RenderType, LayerBuilder> builders = new LinkedHashMap<>();
        PoseStack poseStack = new PoseStack();

        for (StructureSceneRenderer.Voxel voxel : voxels) {
            BlockState state = stateOf(voxel);
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            poseStack.pushPose();
            poseStack.translate(voxel.x(), voxel.y(), voxel.z());
            if (voxel.highlighted()) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.scale(1.04F, 1.04F, 1.04F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }
            BlockPos pos = new BlockPos(voxel.x(), voxel.y(), voxel.z());
            random.setSeed(state.getSeed(pos));
            partsScratch.clear();
            dispatcher.getBlockModel(state).collectParts(random, partsScratch);
            RenderType type = ItemBlockRenderTypes.getRenderType(state);
            LayerBuilder layer = builders.computeIfAbsent(type, LayerBuilder::create);
            dispatcher.renderBatched(state, pos, level, poseStack, layer.consumer, true, partsScratch);
            poseStack.popPose();
        }

        return finalizeLayers(builders);
    }

    private static List<CompiledLayer> finalizeLayers(Map<RenderType, LayerBuilder> builders) {
        if (builders.isEmpty()) return List.of();
        List<CompiledLayer> layers = new ArrayList<>(builders.size());
        for (Map.Entry<RenderType, LayerBuilder> entry : builders.entrySet()) {
            LayerBuilder lb = entry.getValue();
            MeshData mesh = lb.builder.build();
            if (mesh == null) {
                lb.byteBuilder.close();
                continue;
            }
            ByteBuffer src = mesh.vertexBuffer();
            ByteBuffer cached = MemoryUtil.memAlloc(src.remaining());
            cached.put(src.duplicate());
            cached.flip();
            MeshData.DrawState drawState = mesh.drawState();
            mesh.close();
            lb.byteBuilder.close();
            layers.add(new CompiledLayer(entry.getKey(), cached, drawState));
        }
        return layers;
    }

    private static BlockState stateOf(StructureSceneRenderer.Voxel voxel) {
        BlockState state = Block.stateById(voxel.blockStateId());
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    record Cell(int pieceIndex, int y) {}

    static final class StaticMesh implements AutoCloseable {
        final Map<Cell, List<CompiledLayer>> stackByCell;
        final Map<Cell, List<CompiledLayer>> exposeByCell;

        StaticMesh(Map<Cell, List<CompiledLayer>> stackByCell, Map<Cell, List<CompiledLayer>> exposeByCell) {
            this.stackByCell = stackByCell;
            this.exposeByCell = exposeByCell;
        }

        @Override
        public void close() {
            closeAll(stackByCell.values());
            closeAll(exposeByCell.values());
        }

        private static void closeAll(Iterable<List<CompiledLayer>> groups) {
            for (List<CompiledLayer> layers : groups)
                for (CompiledLayer l : layers) l.close();
        }
    }

    static final class CompiledLayer implements AutoCloseable {
        final RenderType renderType;
        final ByteBuffer vertexBytes;
        final MeshData.DrawState drawState;

        CompiledLayer(RenderType renderType, ByteBuffer vertexBytes, MeshData.DrawState drawState) {
            this.renderType = renderType;
            this.vertexBytes = vertexBytes;
            this.drawState = drawState;
        }

        @Override
        public void close() {
            MemoryUtil.memFree(vertexBytes);
        }
    }

    private static final class LayerBuilder {
        final ByteBufferBuilder byteBuilder;
        final BufferBuilder builder;
        final VertexConsumer consumer;

        private LayerBuilder(ByteBufferBuilder byteBuilder, BufferBuilder builder) {
            this.byteBuilder = byteBuilder;
            this.builder = builder;
            this.consumer = builder;
        }

        static LayerBuilder create(RenderType type) {
            ByteBufferBuilder bb = new ByteBufferBuilder(Math.max(786432, type.bufferSize()));
            BufferBuilder builder = new BufferBuilder(bb, type.mode(), type.format());
            return new LayerBuilder(bb, builder);
        }
    }

    private StructureSceneTessellator() {}
}
