package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import fr.hardel.asset_editor.client.rendering.StructureSceneTessellator.AnimatingMesh;
import fr.hardel.asset_editor.client.rendering.StructureSceneTessellator.StaticMesh;

/**
 * Off-screen scene renderer for the Structure viewer. Static voxels are cached per Y level
 * (stack/expose variants) so the slice slider is free at draw time; the animating piece is a
 * separate cache drawn with a translation offset. Tessellation runs in background threads via
 * {@link BackgroundTessQueue}; the GPU draw + read-back happens inside {@link #tick()} on the
 * render thread.
 */
public final class StructureSceneRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSceneRenderer.class);
    private static final int STATIC_CACHE_CAPACITY = 8;
    private static final int ANIMATING_CACHE_CAPACITY = 32;

    private static final AtomicReference<Request> pendingRequest = new AtomicReference<>();
    private static final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private static final Map<Integer, StaticMesh> staticCache = lruCache(STATIC_CACHE_CAPACITY);
    private static final Map<Integer, AnimatingMesh> animatingCache = lruCache(ANIMATING_CACHE_CAPACITY);

    private static final BackgroundTessQueue<Request, StaticMesh> staticTess = new BackgroundTessQueue<>(
        "StructureScene-Tess-Static", LOGGER,
        r -> r.staticKey().hashCode(),
        staticCache::containsKey,
        StructureSceneTessellator::tessellateStatic
    );

    private static final BackgroundTessQueue<Request, AnimatingMesh> animatingTess = new BackgroundTessQueue<>(
        "StructureScene-Tess-Animating", LOGGER,
        r -> r.animatingKey().hashCode(),
        animatingCache::containsKey,
        StructureSceneTessellator::tessellateAnimating
    );

    private static volatile Result result;

    public record Camera(float yaw, float pitch, float zoom, float panX, float panY) {}

    public record Voxel(Identifier blockId, int blockStateId, int x, int y, int z, boolean animating, boolean highlighted) {}

    public record Request(
        String key, String staticKey, String animatingKey,
        int width, int height,
        int sizeX, int sizeY, int sizeZ,
        List<Voxel> voxels,
        int sliceY, float pieceOffset,
        Camera camera
    ) {}

    public record Result(String key, int width, int height, int[] argbPixels) {}

    public static void request(Request request) {
        if (request.width() <= 0 || request.height() <= 0) return;
        pendingRequest.set(request);
    }

    public static Result getResult(String key) {
        Result current = result;
        return (current != null && current.key().equals(key)) ? current : null;
    }

    public static Runnable subscribe(Consumer<String> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public static void tick() {
        Request request = pendingRequest.getAndSet(null);
        if (request == null) return;
        RenderSystem.assertOnRenderThread();
        try {
            render(request);
        } catch (Exception e) {
            LOGGER.error("Structure scene render failed for {}", request.key(), e);
        }
    }

    public static void dispose() {
        pendingRequest.set(null);
        result = null;
        staticTess.clear();
        animatingTess.clear();
        drainAndClose(staticCache);
        drainAndClose(animatingCache);
    }

    private static void render(Request request) {
        staticTess.drainCompletedTo(StructureSceneRenderer::installStatic);
        animatingTess.drainCompletedTo(StructureSceneRenderer::installAnimating);

        boolean wantStatic = anyVoxelMatching(request, false);
        boolean wantAnimating = !request.animatingKey().isEmpty() && anyVoxelMatching(request, true);

        StaticMesh staticMesh = wantStatic ? staticCache.get(request.staticKey().hashCode()) : StructureSceneTessellator.EMPTY_STATIC;
        AnimatingMesh animatingMesh = wantAnimating ? animatingCache.get(request.animatingKey().hashCode()) : StructureSceneTessellator.EMPTY_ANIMATING;

        if (wantStatic && staticMesh == null) {
            staticTess.schedule(request);
            pendingRequest.compareAndSet(null, request);
            return;
        }
        if (wantAnimating && animatingMesh == null) {
            animatingTess.schedule(request);
            pendingRequest.compareAndSet(null, request);
            return;
        }

        StructureSceneFrameDrawer.drawAndReadback(request, staticMesh, animatingMesh, r -> {
            result = r;
            for (Consumer<String> listener : listeners) listener.accept(r.key());
        });
    }

    private static void installStatic(int hash, StaticMesh mesh) {
        StaticMesh old = staticCache.put(hash, mesh);
        if (old != null && old != mesh) closeQuietly(old);
    }

    private static void installAnimating(int hash, AnimatingMesh mesh) {
        AnimatingMesh old = animatingCache.put(hash, mesh);
        if (old != null && old != mesh) closeQuietly(old);
    }

    private static boolean anyVoxelMatching(Request r, boolean animating) {
        for (Voxel v : r.voxels()) if (v.animating() == animating) return true;
        return false;
    }

    private static <V extends AutoCloseable> Map<Integer, V> lruCache(int capacity) {
        return Collections.synchronizedMap(new LinkedHashMap<Integer, V>(capacity + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, V> eldest) {
                if (size() <= capacity) return false;
                closeQuietly(eldest.getValue());
                return true;
            }
        });
    }

    private static <V extends AutoCloseable> void drainAndClose(Map<?, V> cache) {
        synchronized (cache) {
            for (V v : cache.values()) closeQuietly(v);
            cache.clear();
        }
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try { c.close(); } catch (Exception ignored) {}
    }

    private StructureSceneRenderer() {}
}
