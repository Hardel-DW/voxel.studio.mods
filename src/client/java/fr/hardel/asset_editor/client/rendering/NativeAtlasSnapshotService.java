package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import fr.hardel.asset_editor.mixin.client.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reads back native Minecraft {@link TextureAtlas} textures from the GPU
 * so they can be displayed in the Compose debug UI.
 */
public final class NativeAtlasSnapshotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAtlasSnapshotService.class);
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static final AtomicReference<Identifier> pendingRequest = new AtomicReference<>();

    private static volatile Snapshot currentSnapshot;

    public record Snapshot(
        Identifier atlasId,
        int width,
        int height,
        int[] argbPixels,
        Map<Identifier, TextureAtlasSprite> sprites
    ) {}

    public static Snapshot getSnapshot() {
        return currentSnapshot;
    }

    public static void requestSnapshot(Identifier atlasId) {
        pendingRequest.set(atlasId);
    }

    public static Runnable subscribe(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Called from render thread (GameRendererMixin) each frame.
     */
    public static void tick() {
        Identifier requested = pendingRequest.getAndSet(null);
        if (requested == null) return;

        RenderSystem.assertOnRenderThread();
        capture(requested);
    }

    private static void capture(Identifier atlasId) {
        Minecraft mc = Minecraft.getInstance();
        TextureAtlas atlas;
        try {
            atlas = mc.getAtlasManager().getAtlasOrThrow(atlasId);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown atlas: {}", atlasId);
            return;
        }

        TextureAtlasAccessor accessor = (TextureAtlasAccessor) atlas;
        int width = accessor.asset_editor$getWidth();
        int height = accessor.asset_editor$getHeight();
        Map<Identifier, TextureAtlasSprite> sprites = accessor.asset_editor$getTexturesByName();

        if (width <= 0 || height <= 0) {
            LOGGER.warn("Atlas {} has invalid dimensions: {}x{}", atlasId, width, height);
            return;
        }

        GpuTexture texture = atlas.getTexture();
        GpuDevice device = RenderSystem.getDevice();

        int bufferSize = width * height * 4;
        GpuBuffer readbackBuffer = device.createBuffer(
            () -> "AtlasSnapshot/" + atlasId,
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ,
            bufferSize
        );

        device.createCommandEncoder().copyTextureToBuffer(texture, readbackBuffer, 0, () -> {
            try (GpuBuffer.MappedView view = device.createCommandEncoder().mapBuffer(readbackBuffer, true, false)) {
                ByteBuffer pixels = view.data();
                int[] argb = new int[width * height];

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int offset = (y * width + x) * 4;
                        int r = pixels.get(offset) & 0xFF;
                        int g = pixels.get(offset + 1) & 0xFF;
                        int b = pixels.get(offset + 2) & 0xFF;
                        int a = pixels.get(offset + 3) & 0xFF;
                        argb[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }

                currentSnapshot = new Snapshot(atlasId, width, height, argb, Map.copyOf(sprites));
                listeners.forEach(Runnable::run);
                LOGGER.info("Atlas snapshot captured: {} ({}x{}, {} sprites)", atlasId, width, height, sprites.size());
            } finally {
                readbackBuffer.close();
            }
        }, 0);
    }

    private NativeAtlasSnapshotService() {}
}
