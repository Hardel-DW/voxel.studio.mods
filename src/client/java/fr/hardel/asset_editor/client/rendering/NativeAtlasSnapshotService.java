package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import fr.hardel.asset_editor.mixin.client.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
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

    public record SpriteRegion(
        Identifier spriteId,
        int sourceX,
        int sourceY,
        int sourceWidth,
        int sourceHeight
    ) {}

    public record Snapshot(
        Identifier atlasId,
        int width,
        int height,
        int[] argbPixels,
        Map<Identifier, SpriteRegion> sprites
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
        Map<Identifier, SpriteRegion> sprites = captureRegions(accessor.asset_editor$getTexturesByName(), width, height);

        if (width <= 0 || height <= 0) {
            LOGGER.warn("Atlas {} has invalid dimensions: {}x{}", atlasId, width, height);
            return;
        }

        GpuTexture texture = atlas.getTexture();
        GpuDevice device = RenderSystem.getDevice();

        GpuReadback.readArgb(device, texture, "AtlasSnapshot/" + atlasId, width, height, false, null,
            argb -> {
                currentSnapshot = new Snapshot(atlasId, width, height, argb, Map.copyOf(sprites));
                listeners.forEach(Runnable::run);
                LOGGER.info("Atlas snapshot captured: {} ({}x{}, {} sprites)", atlasId, width, height, sprites.size());
            });
    }

    private static Map<Identifier, SpriteRegion> captureRegions(
        Map<Identifier, TextureAtlasSprite> sprites,
        int atlasWidth,
        int atlasHeight
    ) {
        LinkedHashMap<Identifier, SpriteRegion> regions = new LinkedHashMap<>(sprites.size());
        sprites.forEach((spriteId, sprite) -> {
            int sourceX = Mth.clamp(Math.round(sprite.getU0() * atlasWidth), 0, atlasWidth);
            int sourceY = Mth.clamp(Math.round(sprite.getV0() * atlasHeight), 0, atlasHeight);
            int sourceWidth = Math.min(sprite.contents().width(), atlasWidth - sourceX);
            int sourceHeight = Math.min(sprite.contents().height(), atlasHeight - sourceY);

            if (sourceWidth <= 0 || sourceHeight <= 0) {
                LOGGER.debug("Skipping empty sprite region {} in atlas {}", spriteId, sprite.atlasLocation());
                return;
            }

            regions.put(spriteId, new SpriteRegion(spriteId, sourceX, sourceY, sourceWidth, sourceHeight));
        });
        return regions;
    }

    private NativeAtlasSnapshotService() {}
}
