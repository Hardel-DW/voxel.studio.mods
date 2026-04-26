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

import fr.hardel.asset_editor.client.rendering.StructureSceneTessellator.StaticMesh;

/**
 * Off-screen scene renderer for the Structure viewer. Static voxels are cached per Y level
 * (stack/expose variants) so the slice slider is free at draw time. Tessellation runs in a
 * background thread via {@link BackgroundTessQueue}; the GPU draw + read-back happens inside
 * {@link #tick()} on the render thread.
 */
public final class StructureSceneRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSceneRenderer.class);
    private static final int STATIC_CACHE_CAPACITY = 8;

    private static final AtomicReference<Request> pendingRequest = new AtomicReference<>();
    private static final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private static final Map<Integer, StaticMesh> staticCache = lruCache(STATIC_CACHE_CAPACITY);

    private static final BackgroundTessQueue<Request, StaticMesh> staticTess = new BackgroundTessQueue<>(
        "StructureScene-Tess-Static", LOGGER,
        r -> r.staticKey().hashCode(),
        staticCache::containsKey,
        StructureSceneTessellator::tessellateStatic
    );

    private static volatile Result result;

    public record Camera(float yaw, float pitch, float zoom, float panX, float panY) {}

    public record Voxel(Identifier blockId, int blockStateId, int x, int y, int z, boolean highlighted) {}

    public record Request(
        String key, String staticKey,
        int width, int height,
        int sizeX, int sizeY, int sizeZ,
        List<Voxel> voxels,
        int sliceY,
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
        drainAndClose(staticCache);
    }

    private static void render(Request request) {
        staticTess.drainCompletedTo(StructureSceneRenderer::installStatic);

        boolean wantStatic = !request.voxels().isEmpty();
        StaticMesh staticMesh = wantStatic ? staticCache.get(request.staticKey().hashCode()) : StructureSceneTessellator.EMPTY_STATIC;

        if (wantStatic && staticMesh == null) {
            staticTess.schedule(request);
            pendingRequest.compareAndSet(null, request);
            return;
        }

        StructureSceneFrameDrawer.drawAndReadback(request, staticMesh, r -> {
            result = r;
            for (Consumer<String> listener : listeners) listener.accept(r.key());
        });
    }

    private static void installStatic(int hash, StaticMesh mesh) {
        StaticMesh old = staticCache.put(hash, mesh);
        if (old != null && old != mesh) closeQuietly(old);
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
