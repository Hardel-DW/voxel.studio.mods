package fr.hardel.asset_editor.client.memory.core;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

public final class SimpleMemory<S> implements MutableMemory<S> {

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile S snapshot;

    public SimpleMemory(S initialSnapshot) {
        this.snapshot = initialSnapshot;
    }

    @Override
    public S snapshot() {
        return snapshot;
    }

    @Override
    public void setSnapshot(S nextSnapshot) {
        synchronized (this) {
            if (Objects.equals(snapshot, nextSnapshot))
                return;

            snapshot = nextSnapshot;
        }
        listeners.forEach(Runnable::run);
    }

    @Override
    public S update(UnaryOperator<S> updater) {
        Objects.requireNonNull(updater, "updater");

        S previous;
        S nextSnapshot;
        synchronized (this) {
            previous = snapshot;
            nextSnapshot = updater.apply(previous);
            if (Objects.equals(previous, nextSnapshot))
                return previous;

            snapshot = nextSnapshot;
        }
        listeners.forEach(Runnable::run);
        return nextSnapshot;
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
