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
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Renders a single item to a framebuffer at an exact requested size.
 * Unlike {@link ItemAtlasRenderer} which produces a shared atlas at a fixed
 * cell size, this renderer targets per-caller quality by asterisking the
 * block model at the display pixel size. Results are cached by (itemId, size).
 */
public final class HighQualityBlockRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HighQualityBlockRenderer.class);

    public record Key(Identifier itemId, int size) {}

    public record Result(int[] argbPixels, int width, int height) {}

    private static final Queue<Key> pendingQueue = new ConcurrentLinkedQueue<>();
    private static final Set<Key> pendingSet = ConcurrentHashMap.newKeySet();
    private static final Map<Key, Result> results = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<Consumer<Key>> listeners = new CopyOnWriteArrayList<>();

    public static void request(Identifier itemId, int size) {
        if (size <= 0 || size > 1024)
            return;
        Key key = new Key(itemId, size);
        if (results.containsKey(key))
            return;
        if (pendingSet.add(key))
            pendingQueue.add(key);
    }

    public static Result getResult(Key key) {
        return results.get(key);
    }

    public static Runnable subscribe(Consumer<Key> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public static void tryRenderPending() {
        RenderSystem.assertOnRenderThread();
        Key key;
        while ((key = pendingQueue.poll()) != null) {
            try {
                renderSingle(key);
            } catch (Exception exception) {
                LOGGER.error("HQ block render failed for {} @ {}", key.itemId(), key.size(), exception);
                pendingSet.remove(key);
            }
        }
    }

    private static void renderSingle(Key key) {
        int size = key.size();
        Minecraft mc = Minecraft.getInstance();
        Item item = BuiltInRegistries.ITEM.getValue(key.itemId());
        ItemModelResolver modelResolver = mc.getItemModelResolver();

        TrackingItemStackRenderState renderState = new TrackingItemStackRenderState();
        modelResolver.updateForTopItem(renderState, new ItemStack(item), ItemDisplayContext.GUI, mc.level, null, 0);
        if (renderState.isEmpty()) {
            pendingSet.remove(key);
            return;
        }

        GpuDevice device = RenderSystem.getDevice();
        GpuTexture colorTexture = device.createTexture("HQBlock/Color", 12 | GpuTexture.USAGE_COPY_SRC, TextureFormat.RGBA8, size, size, 1, 1);
        GpuTexture depthTexture = device.createTexture("HQBlock/Depth", 8, TextureFormat.DEPTH32, size, size, 1, 1);
        GpuTextureView colorView = device.createTextureView(colorTexture);
        GpuTextureView depthView = device.createTextureView(depthTexture);

        CommandEncoder encoder = device.createCommandEncoder();
        encoder.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        RenderSystem.backupProjectionMatrix();
        var savedColorOverride = RenderSystem.outputColorTextureOverride;
        var savedDepthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;

        CachedOrthoProjectionMatrixBuffer projBuffer = new CachedOrthoProjectionMatrixBuffer("hqBlock", -1000.0F, 1000.0F, true);
        RenderSystem.setProjectionMatrix(projBuffer.getBuffer(size, size), ProjectionType.ORTHOGRAPHIC);

        SubmitNodeStorage submitStorage = new SubmitNodeStorage();
        var fixedBuffers = new Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>();
        fixedBuffers.put(Sheets.translucentItemSheet(), new ByteBufferBuilder(786432));
        fixedBuffers.put(RenderTypes.armorEntityGlint(), new ByteBufferBuilder(RenderTypes.armorEntityGlint().bufferSize()));
        fixedBuffers.put(RenderTypes.glint(), new ByteBufferBuilder(RenderTypes.glint().bufferSize()));
        fixedBuffers.put(RenderTypes.glintTranslucent(), new ByteBufferBuilder(RenderTypes.glintTranslucent().bufferSize()));
        fixedBuffers.put(RenderTypes.entityGlint(), new ByteBufferBuilder(RenderTypes.entityGlint().bufferSize()));
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new ByteBufferBuilder(786432));
        FeatureRenderDispatcher featureDispatcher = new FeatureRenderDispatcher(
            submitStorage,
            mc.getBlockRenderer(),
            bufferSource,
            mc.getAtlasManager(),
            new OutlineBufferSource(),
            MultiBufferSource.immediate(new ByteBufferBuilder(1536)),
            mc.font);

        boolean flat = !renderState.usesBlockLight();
        mc.gameRenderer.getLighting().setupFor(flat ? Lighting.Entry.ITEMS_FLAT : Lighting.Entry.ITEMS_3D);

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(size / 2.0F, size / 2.0F, 0.0F);
        poseStack.scale(size, -size, size);

        RenderSystem.enableScissorForRenderTypeDraws(0, 0, size, size);
        renderState.submit(poseStack, submitStorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
        featureDispatcher.renderAllFeatures();
        bufferSource.endBatch();
        RenderSystem.disableScissorForRenderTypeDraws();

        poseStack.popPose();

        RenderSystem.outputColorTextureOverride = savedColorOverride;
        RenderSystem.outputDepthTextureOverride = savedDepthOverride;
        RenderSystem.restoreProjectionMatrix();
        projBuffer.close();

        depthView.close();
        depthTexture.close();

        readback(device, colorTexture, colorView, key);
    }

    private static void readback(GpuDevice device, GpuTexture colorTexture, GpuTextureView colorView, Key key) {
        int size = key.size();
        int bufferSize = size * size * 4;
        GpuBuffer readbackBuffer = device.createBuffer(() -> "HQBlock/Readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);

        device.createCommandEncoder().copyTextureToBuffer(colorTexture, readbackBuffer, 0, () -> {
            try (GpuBuffer.MappedView view = device.createCommandEncoder().mapBuffer(readbackBuffer, true, false)) {
                ByteBuffer pixels = view.data();
                int[] result = new int[size * size];
                for (int srcY = 0; srcY < size; srcY++) {
                    int dstY = size - 1 - srcY;
                    for (int x = 0; x < size; x++) {
                        int srcOffset = (srcY * size + x) * 4;
                        int r = pixels.get(srcOffset) & 0xFF;
                        int g = pixels.get(srcOffset + 1) & 0xFF;
                        int b = pixels.get(srcOffset + 2) & 0xFF;
                        int a = pixels.get(srcOffset + 3) & 0xFF;
                        result[dstY * size + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                results.put(key, new Result(result, size, size));
                pendingSet.remove(key);
                for (Consumer<Key> listener : listeners)
                    listener.accept(key);
            } finally {
                readbackBuffer.close();
                colorView.close();
                colorTexture.close();
            }
        }, 0);
    }

    private HighQualityBlockRenderer() {}
}
