package fr.hardel.asset_editor.client.rendering;

import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

/**
 * Single-worker tessellation queue. Coalesces requests (only the latest pending
 * request is kept), skips work whose key is already cached, and parks completed
 * meshes in an EDT-/render-thread-drained queue so cache writes happen on a
 * single owner thread.
 */
public final class BackgroundTessQueue<R, M> {

    private final String name;
    private final Logger logger;
    private final ToIntFunction<R> hashFn;
    private final IntPredicate alreadyCached;
    private final Function<R, M> tessellator;
    private final AtomicReference<R> queued = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Queue<Completion<M>> completed = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;

    public BackgroundTessQueue(
        String threadName,
        Logger logger,
        ToIntFunction<R> hashFn,
        IntPredicate alreadyCached,
        Function<R, M> tessellator
    ) {
        this.name = threadName;
        this.logger = logger;
        this.hashFn = hashFn;
        this.alreadyCached = alreadyCached;
        this.tessellator = tessellator;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
    }

    public void schedule(R request) {
        int hash = hashFn.applyAsInt(request);
        if (alreadyCached.test(hash)) return;
        queued.set(request);
        if (running.compareAndSet(false, true)) {
            executor.submit(this::loop);
        }
    }

    public void drainCompletedTo(BiConsumer<Integer, M> sink) {
        Completion<M> c;
        while ((c = completed.poll()) != null) sink.accept(c.hash, c.mesh);
    }

    public void clear() {
        queued.set(null);
        completed.clear();
    }

    private void loop() {
        try {
            while (true) {
                R next = queued.getAndSet(null);
                if (next == null) {
                    running.set(false);
                    if (queued.get() == null || !running.compareAndSet(false, true)) return;
                    continue;
                }
                int hash = hashFn.applyAsInt(next);
                if (alreadyCached.test(hash)) continue;
                try {
                    completed.offer(new Completion<>(hash, tessellator.apply(next)));
                } catch (Throwable t) {
                    logger.error("{}: tessellation failed for hash {}", name, hash, t);
                }
            }
        } catch (Throwable t) {
            logger.error("{}: loop crashed", name, t);
            running.set(false);
        }
    }

    private record Completion<M>(int hash, M mesh) {}
}
