package fr.hardel.asset_editor.client.memory.session.ui;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reactive wrapper around the persistent user preferences exposed by {@code ClientPreferences}.
 *
 * Constructor takes a loader + one persist callback per field so the memory layer stays
 * decoupled from the concrete storage and a single-field mutation only rewrites that field.
 * Mirrors the supplier/consumer contract already used by {@link PackSelectionMemory}.
 *
 * Adding a new toggle: grow {@link Snapshot}, add a setter that rebuilds the snapshot with
 * the new value + invokes its persist callback, extend the constructor.
 */
public final class SettingsMemory implements ReadableMemory<SettingsMemory.Snapshot> {

    public record Snapshot(
        boolean showFpsCounter,
        boolean disableVsync,
        boolean stayOnSplash,
        boolean showHoverTriangle
    ) { }

    private final Consumer<Boolean> persistShowFpsCounter;
    private final Consumer<Boolean> persistDisableVsync;
    private final Consumer<Boolean> persistStayOnSplash;
    private final Consumer<Boolean> persistShowHoverTriangle;
    private final SimpleMemory<Snapshot> memory;

    public SettingsMemory(
        Supplier<Snapshot> loader,
        Consumer<Boolean> persistShowFpsCounter,
        Consumer<Boolean> persistDisableVsync,
        Consumer<Boolean> persistStayOnSplash,
        Consumer<Boolean> persistShowHoverTriangle
    ) {
        this.persistShowFpsCounter = persistShowFpsCounter;
        this.persistDisableVsync = persistDisableVsync;
        this.persistStayOnSplash = persistStayOnSplash;
        this.persistShowHoverTriangle = persistShowHoverTriangle;
        this.memory = new SimpleMemory<>(loader.get());
    }

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public void setShowFpsCounter(boolean value) {
        Snapshot prev = memory.snapshot();
        if (prev.showFpsCounter() == value) return;
        memory.setSnapshot(new Snapshot(value, prev.disableVsync(), prev.stayOnSplash(), prev.showHoverTriangle()));
        persistShowFpsCounter.accept(value);
    }

    public void setDisableVsync(boolean value) {
        Snapshot prev = memory.snapshot();
        if (prev.disableVsync() == value) return;
        memory.setSnapshot(new Snapshot(prev.showFpsCounter(), value, prev.stayOnSplash(), prev.showHoverTriangle()));
        persistDisableVsync.accept(value);
    }

    public void setStayOnSplash(boolean value) {
        Snapshot prev = memory.snapshot();
        if (prev.stayOnSplash() == value) return;
        memory.setSnapshot(new Snapshot(prev.showFpsCounter(), prev.disableVsync(), value, prev.showHoverTriangle()));
        persistStayOnSplash.accept(value);
    }

    public void setShowHoverTriangle(boolean value) {
        Snapshot prev = memory.snapshot();
        if (prev.showHoverTriangle() == value) return;
        memory.setSnapshot(new Snapshot(prev.showFpsCounter(), prev.disableVsync(), prev.stayOnSplash(), value));
        persistShowHoverTriangle.accept(value);
    }
}
