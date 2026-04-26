package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Off-screen scene renderer for the Structure viewer.
 *
 * <h2>Architecture (post Phase A+C refactor)</h2>
 *
 * Two independent caches keyed on what actually drives geometry:
 * <ul>
 *   <li><b>staticCache</b> — static structure mesh, indexed per Y level. Slice Y, drop offset and
 *       camera are <i>not</i> in the cache key — they are applied at draw time. Each Y level is
 *       tessellated twice: a {@code stack} variant (full level — top hidden by neighbour above)
 *       and an {@code expose} variant (level truncated above Y — top exposed). Drawing the
 *       slice plane combines stack[0..sliceY-1] + expose[sliceY], so the user keeps the
 *       open-top "look inside" behaviour for free.</li>
 *   <li><b>animatingCache</b> — single mesh for the currently animating piece, regardless of Y
 *       slicing. The drop offset is applied as a {@link Matrix4fStack} translate at draw time,
 *       so animation frames hit the cache at every interpolation step.</li>
 * </ul>
 *
 * Two single-thread executors run static and animating tessellation in parallel — the animating
 * piece is small (~one piece) so it almost always finishes within one frame, which means the
 * user sees the falling piece immediately even when the static mesh is mid-build.
 *
 * The Compose-side {@link Request} carries the full voxel list (no slice filter) plus a {@code sliceY}
 * field that is consumed only at draw time.
 */
public final class StructureSceneRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSceneRenderer.class);
    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    private static final float NEAR_PLANE = -32768.0F;
    private static final float FAR_PLANE = 32768.0F;
    private static final int STATIC_CACHE_CAPACITY = 8;
    private static final int ANIMATING_CACHE_CAPACITY = 32;

    private static final AtomicReference<Request> pendingRequest = new AtomicReference<>();

    private static final AtomicReference<Request> queuedStaticTess = new AtomicReference<>();
    private static final AtomicBoolean staticTessRunning = new AtomicBoolean(false);
    private static final ExecutorService staticTessExecutor = Executors.newSingleThreadExecutor(r -> daemon(r, "StructureScene-Tess-Static"));
    private static final Queue<CompletedStatic> completedStatic = new ConcurrentLinkedQueue<>();

    private static final AtomicReference<Request> queuedAnimatingTess = new AtomicReference<>();
    private static final AtomicBoolean animatingTessRunning = new AtomicBoolean(false);
    private static final ExecutorService animatingTessExecutor = Executors.newSingleThreadExecutor(r -> daemon(r, "StructureScene-Tess-Animating"));
    private static final Queue<CompletedAnimating> completedAnimating = new ConcurrentLinkedQueue<>();

    private static final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private static final Map<Integer, StaticMesh> staticCache = Collections.synchronizedMap(new LinkedHashMap<>(STATIC_CACHE_CAPACITY + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, StaticMesh> eldest) {
            if (size() > STATIC_CACHE_CAPACITY) {
                eldest.getValue().close();
                return true;
            }
            return false;
        }
    });

    private static final Map<Integer, AnimatingMesh> animatingCache = Collections.synchronizedMap(new LinkedHashMap<>(ANIMATING_CACHE_CAPACITY + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, AnimatingMesh> eldest) {
            if (size() > ANIMATING_CACHE_CAPACITY) {
                eldest.getValue().close();
                return true;
            }
            return false;
        }
    });

    private static final StaticMesh EMPTY_STATIC = new StaticMesh(Map.of(), Map.of());
    private static final AnimatingMesh EMPTY_ANIMATING = new AnimatingMesh(List.of());

    private static volatile Result result;

    private record CompletedStatic(int hash, StaticMesh mesh) {}
    private record CompletedAnimating(int hash, AnimatingMesh mesh) {}

    public record Camera(float yaw, float pitch, float zoom, float panX, float panY) {}

    public record Voxel(Identifier blockId, int blockStateId, int x, int y, int z, boolean animating, boolean highlighted) {}

    /**
     * @param key            Image key — distinct per visible frame (camera + viewport + sliceY + drop).
     * @param staticKey      Static-mesh cache key — covers everything that affects the static
     *                       geometry (subject, displayed stages, animating exclusion, jigsaws,
     *                       highlight). Excludes sliceY/drop/camera.
     * @param animatingKey   Animating-mesh cache key, or empty string when no piece is animating.
     * @param voxels         Full voxel list, NOT pre-filtered by sliceY. The renderer slices at draw time.
     * @param sliceY         Vertical clip — applied at draw time only.
     * @param pieceOffset    Drop offset for the animating mesh — applied via Matrix4fStack.
     */
    public record Request(
        String key,
        String staticKey,
        String animatingKey,
        int width,
        int height,
        int sizeX,
        int sizeY,
        int sizeZ,
        List<Voxel> voxels,
        int sliceY,
        float pieceOffset,
        Camera camera
    ) {}

    public record Result(String key, int width, int height, int[] argbPixels) {}

    public static void request(Request request) {
        if (request.width() <= 0 || request.height() <= 0) {
            return;
        }
        pendingRequest.set(request);
    }

    public static Result getResult(String key) {
        Result current = result;
        if (current == null || !current.key().equals(key)) {
            return null;
        }
        return current;
    }

    public static Runnable subscribe(Consumer<String> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public static void tick() {
        Request request = pendingRequest.getAndSet(null);
        if (request == null) {
            return;
        }
        RenderSystem.assertOnRenderThread();
        try {
            render(request);
        } catch (Exception exception) {
            LOGGER.error("Structure scene render failed for {}", request.key(), exception);
        }
    }

    private static void render(Request request) {
        drainCompleted();

        boolean wantStatic = hasStaticVoxels(request);
        boolean wantAnimating = !request.animatingKey().isEmpty() && hasAnimatingVoxels(request);

        StaticMesh staticMesh = wantStatic ? staticCache.get(request.staticKey().hashCode()) : EMPTY_STATIC;
        AnimatingMesh animatingMesh = wantAnimating ? animatingCache.get(request.animatingKey().hashCode()) : EMPTY_ANIMATING;

        if (wantStatic && staticMesh == null) {
            scheduleStaticTess(request);
            pendingRequest.compareAndSet(null, request);
            staticMesh = EMPTY_STATIC;
        }
        if (wantAnimating && animatingMesh == null) {
            scheduleAnimatingTess(request);
            pendingRequest.compareAndSet(null, request);
            animatingMesh = EMPTY_ANIMATING;
        }

        // If both the static cache *and* the animating cache missed for the very first frame,
        // we have nothing to show yet — let the next tick try again instead of producing a
        // blank readback that would overwrite the previous frame.
        if (wantStatic && staticMesh == EMPTY_STATIC && wantAnimating && animatingMesh == EMPTY_ANIMATING) {
            return;
        }

        drawAndReadback(request, staticMesh, animatingMesh);
    }

    private static boolean hasStaticVoxels(Request r) {
        for (Voxel v : r.voxels()) if (!v.animating()) return true;
        return false;
    }

    private static boolean hasAnimatingVoxels(Request r) {
        for (Voxel v : r.voxels()) if (v.animating()) return true;
        return false;
    }

    private static void drawAndReadback(Request request, StaticMesh staticMesh, AnimatingMesh animatingMesh) {
        int width = Math.min(request.width(), MAX_WIDTH);
        int height = Math.min(request.height(), MAX_HEIGHT);

        GpuDevice device = RenderSystem.getDevice();
        GpuTexture colorTexture = device.createTexture("StructureScene/Color", 12 | GpuTexture.USAGE_COPY_SRC, TextureFormat.RGBA8, width, height, 1, 1);
        GpuTexture depthTexture = device.createTexture("StructureScene/Depth", 8, TextureFormat.DEPTH32, width, height, 1, 1);
        GpuTextureView colorView = device.createTextureView(colorTexture);
        GpuTextureView depthView = device.createTextureView(depthTexture);

        CommandEncoder encoder = device.createCommandEncoder();
        encoder.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        RenderSystem.backupProjectionMatrix();
        var savedColorOverride = RenderSystem.outputColorTextureOverride;
        var savedDepthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;

        CachedOrthoProjectionMatrixBuffer projection = new CachedOrthoProjectionMatrixBuffer("structureScene", NEAR_PLANE, FAR_PLANE, true);
        RenderSystem.setProjectionMatrix(projection.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(0, 0, width, height);

        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);

        drawScene(staticMesh, animatingMesh, request, width, height);

        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = savedColorOverride;
        RenderSystem.outputDepthTextureOverride = savedDepthOverride;
        RenderSystem.restoreProjectionMatrix();
        projection.close();
        depthView.close();
        depthTexture.close();

        readback(device, colorTexture, colorView, request.key(), width, height);
    }

    private static void drawScene(StaticMesh staticMesh, AnimatingMesh animatingMesh, Request request, int width, int height) {
        if (staticMesh.stackByY.isEmpty() && staticMesh.exposeByY.isEmpty() && animatingMesh.layers.isEmpty()) {
            return;
        }
        Matrix4fStack mvm = RenderSystem.getModelViewStack();
        mvm.pushMatrix();
        applyCameraToMvm(mvm, request, width, height);
        try {
            int sliceY = request.sliceY();
            if (sliceY >= 0) {
                for (int y = 0; y < sliceY; y++) {
                    List<CompiledLayer> layers = staticMesh.stackByY.get(y);
                    if (layers != null) {
                        for (CompiledLayer layer : layers) drawLayer(layer);
                    }
                }
                List<CompiledLayer> top = staticMesh.exposeByY.get(sliceY);
                if (top != null) {
                    for (CompiledLayer layer : top) drawLayer(layer);
                }
            }
            if (!animatingMesh.layers.isEmpty()) {
                float offset = request.pieceOffset();
                if (offset != 0f) {
                    mvm.pushMatrix();
                    mvm.translate(0f, offset, 0f);
                    try {
                        for (CompiledLayer layer : animatingMesh.layers) drawLayer(layer);
                    } finally {
                        mvm.popMatrix();
                    }
                } else {
                    for (CompiledLayer layer : animatingMesh.layers) drawLayer(layer);
                }
            }
        } finally {
            mvm.popMatrix();
        }
    }

    private static void scheduleStaticTess(Request request) {
        if (staticCache.containsKey(request.staticKey().hashCode())) return;
        queuedStaticTess.set(request);
        if (staticTessRunning.compareAndSet(false, true)) {
            staticTessExecutor.submit(StructureSceneRenderer::staticTessLoop);
        }
    }

    private static void scheduleAnimatingTess(Request request) {
        if (animatingCache.containsKey(request.animatingKey().hashCode())) return;
        queuedAnimatingTess.set(request);
        if (animatingTessRunning.compareAndSet(false, true)) {
            animatingTessExecutor.submit(StructureSceneRenderer::animatingTessLoop);
        }
    }

    private static void staticTessLoop() {
        try {
            while (true) {
                Request next = queuedStaticTess.getAndSet(null);
                if (next == null) {
                    staticTessRunning.set(false);
                    if (queuedStaticTess.get() == null || !staticTessRunning.compareAndSet(false, true)) return;
                    continue;
                }
                int hash = next.staticKey().hashCode();
                if (staticCache.containsKey(hash)) continue;
                try {
                    StaticMesh mesh = tessellateStatic(next);
                    completedStatic.offer(new CompletedStatic(hash, mesh));
                } catch (Throwable t) {
                    LOGGER.error("Static tessellation failed for hash {}", hash, t);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Static tessellation loop crashed", t);
            staticTessRunning.set(false);
        }
    }

    private static void animatingTessLoop() {
        try {
            while (true) {
                Request next = queuedAnimatingTess.getAndSet(null);
                if (next == null) {
                    animatingTessRunning.set(false);
                    if (queuedAnimatingTess.get() == null || !animatingTessRunning.compareAndSet(false, true)) return;
                    continue;
                }
                int hash = next.animatingKey().hashCode();
                if (animatingCache.containsKey(hash)) continue;
                try {
                    AnimatingMesh mesh = tessellateAnimating(next);
                    completedAnimating.offer(new CompletedAnimating(hash, mesh));
                } catch (Throwable t) {
                    LOGGER.error("Animating tessellation failed for hash {}", hash, t);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Animating tessellation loop crashed", t);
            animatingTessRunning.set(false);
        }
    }

    private static void drainCompleted() {
        CompletedStatic s;
        while ((s = completedStatic.poll()) != null) {
            staticCache.put(s.hash(), s.mesh());
        }
        CompletedAnimating a;
        while ((a = completedAnimating.poll()) != null) {
            animatingCache.put(a.hash(), a.mesh());
        }
    }

    private static StaticMesh tessellateStatic(Request request) {
        Map<Integer, List<Voxel>> byY = new HashMap<>();
        List<Voxel> staticOnly = new ArrayList<>();
        for (Voxel v : request.voxels()) {
            if (v.animating()) continue;
            staticOnly.add(v);
            byY.computeIfAbsent(v.y(), k -> new ArrayList<>()).add(v);
        }
        if (byY.isEmpty()) return EMPTY_STATIC;

        StructureBlockView fullLevel = new StructureBlockView(staticOnly, request.sizeY(), Integer.MAX_VALUE);

        Map<Integer, List<CompiledLayer>> stackByY = new HashMap<>();
        Map<Integer, List<CompiledLayer>> exposeByY = new HashMap<>();

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        RandomSource random = RandomSource.create();
        List<BlockModelPart> parts = new ArrayList<>();

        for (Map.Entry<Integer, List<Voxel>> entry : byY.entrySet()) {
            int y = entry.getKey();
            List<Voxel> voxelsAtY = entry.getValue();
            stackByY.put(y, tessellateLayer(voxelsAtY, fullLevel, dispatcher, random, parts));
            StructureBlockView truncated = new StructureBlockView(staticOnly, request.sizeY(), y);
            exposeByY.put(y, tessellateLayer(voxelsAtY, truncated, dispatcher, random, parts));
        }

        return new StaticMesh(stackByY, exposeByY);
    }

    private static AnimatingMesh tessellateAnimating(Request request) {
        List<Voxel> animVoxels = new ArrayList<>();
        List<Voxel> staticOnly = new ArrayList<>();
        for (Voxel v : request.voxels()) {
            if (v.animating()) animVoxels.add(v);
            else staticOnly.add(v);
        }
        if (animVoxels.isEmpty()) return EMPTY_ANIMATING;

        StructureBlockView level = new StructureBlockView(staticOnly, request.sizeY(), Integer.MAX_VALUE);
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        RandomSource random = RandomSource.create();
        List<BlockModelPart> parts = new ArrayList<>();

        return new AnimatingMesh(tessellateLayer(animVoxels, level, dispatcher, random, parts));
    }

    private static List<CompiledLayer> tessellateLayer(
        List<Voxel> voxels,
        BlockAndTintGetter level,
        BlockRenderDispatcher dispatcher,
        RandomSource random,
        List<BlockModelPart> partsScratch
    ) {
        Map<RenderType, LayerBuilder> builders = new LinkedHashMap<>();
        PoseStack poseStack = new PoseStack();

        for (Voxel voxel : voxels) {
            BlockState state = blockState(voxel);
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

    private static void drawLayer(CompiledLayer layer) {
        int size = layer.vertexBytes.remaining();
        if (size == 0) return;
        ByteBufferBuilder builder = new ByteBufferBuilder(size);
        try {
            long ptr = builder.reserve(size);
            ByteBuffer target = MemoryUtil.memByteBuffer(ptr, size);
            target.put(layer.vertexBytes.duplicate());
            ByteBufferBuilder.Result result = builder.build();
            if (result == null) return;
            MeshData mesh = new MeshData(result, layer.drawState);
            layer.renderType.draw(mesh);
        } finally {
            builder.close();
        }
    }

    private static void applyCameraToMvm(Matrix4fStack mvm, Request request, int width, int height) {
        Camera camera = request.camera();
        mvm.translate(width * 0.5F + camera.panX(), height * 0.5F + camera.panY(), 0.0F);
        mvm.scale(camera.zoom(), -camera.zoom(), camera.zoom());
        mvm.rotateX((float) Math.toRadians(camera.pitch()));
        mvm.rotateY((float) Math.toRadians(camera.yaw()));
        mvm.translate(-request.sizeX() * 0.5F, -request.sizeY() * 0.5F, -request.sizeZ() * 0.5F);
    }

    private static BlockState blockState(Voxel voxel) {
        BlockState state = Block.stateById(voxel.blockStateId());
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    private static void readback(GpuDevice device, GpuTexture colorTexture, GpuTextureView colorView, String key, int width, int height) {
        int bufferSize = width * height * 4;
        GpuBuffer readbackBuffer = device.createBuffer(() -> "StructureScene/Readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);

        device.createCommandEncoder().copyTextureToBuffer(colorTexture, readbackBuffer, 0, () -> {
            try (GpuBuffer.MappedView view = device.createCommandEncoder().mapBuffer(readbackBuffer, true, false)) {
                ByteBuffer pixels = view.data();
                int[] argb = new int[width * height];
                for (int srcY = 0; srcY < height; srcY++) {
                    int dstY = height - 1 - srcY;
                    for (int x = 0; x < width; x++) {
                        int offset = (srcY * width + x) * 4;
                        int r = pixels.get(offset) & 0xFF;
                        int g = pixels.get(offset + 1) & 0xFF;
                        int b = pixels.get(offset + 2) & 0xFF;
                        int a = pixels.get(offset + 3) & 0xFF;
                        argb[dstY * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                result = new Result(key, width, height, argb);
                for (Consumer<String> listener : listeners) {
                    listener.accept(key);
                }
            } finally {
                readbackBuffer.close();
                colorView.close();
                colorTexture.close();
            }
        }, 0);
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    private static final class StaticMesh implements AutoCloseable {
        final Map<Integer, List<CompiledLayer>> stackByY;
        final Map<Integer, List<CompiledLayer>> exposeByY;

        StaticMesh(Map<Integer, List<CompiledLayer>> stackByY, Map<Integer, List<CompiledLayer>> exposeByY) {
            this.stackByY = stackByY;
            this.exposeByY = exposeByY;
        }

        @Override
        public void close() {
            for (List<CompiledLayer> layers : stackByY.values()) for (CompiledLayer l : layers) l.close();
            for (List<CompiledLayer> layers : exposeByY.values()) for (CompiledLayer l : layers) l.close();
        }
    }

    private static final class AnimatingMesh implements AutoCloseable {
        final List<CompiledLayer> layers;

        AnimatingMesh(List<CompiledLayer> layers) {
            this.layers = layers;
        }

        @Override
        public void close() {
            for (CompiledLayer l : layers) l.close();
        }
    }

    private static final class CompiledLayer implements AutoCloseable {
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

    private static final class StructureBlockView implements BlockAndTintGetter {
        private final Map<Long, BlockState> states;
        private final int height;
        private final int maxYInclusive;

        StructureBlockView(List<Voxel> voxels, int height, int maxYInclusive) {
            this.height = Math.max(1, height + 2);
            this.maxYInclusive = maxYInclusive;
            this.states = new HashMap<>(Math.max(16, voxels.size()));
            for (Voxel voxel : voxels) {
                states.put(BlockPos.asLong(voxel.x(), voxel.y(), voxel.z()), blockState(voxel));
            }
        }

        @Override
        public float getShade(@NotNull Direction direction, boolean shade) {
            if (!shade) return 1.0F;
            return switch (direction) {
                case DOWN -> 0.45F;
                case UP -> 1.0F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
            };
        }

        @Override public LevelLightEngine getLightEngine() { return LevelLightEngine.EMPTY; }
        @Override public int getBrightness(@NotNull LightLayer layer, @NotNull BlockPos pos) { return 15; }
        @Override public int getRawBrightness(@NotNull BlockPos pos, int darkening) { return 15; }
        @Override public int getBlockTint(@NotNull BlockPos pos, @NotNull ColorResolver color) { return -1; }
        @Override public @Nullable BlockEntity getBlockEntity(@NotNull BlockPos pos) { return null; }

        @Override
        public @NotNull BlockState getBlockState(@NotNull BlockPos pos) {
            if (pos.getY() > maxYInclusive) return Blocks.AIR.defaultBlockState();
            return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState());
        }

        @Override
        public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
            if (pos.getY() > maxYInclusive) return Blocks.AIR.defaultBlockState().getFluidState();
            return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState()).getFluidState();
        }

        @Override public int getHeight() { return height; }
        @Override public int getMinY() { return 0; }
    }

    private StructureSceneRenderer() {}
}
