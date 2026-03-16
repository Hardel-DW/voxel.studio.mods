package fr.hardel.asset_editor.client.javafx.lib.store;

import fr.hardel.asset_editor.store.ElementEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StoreSelector<R> {

    private final Function<ElementEntry<?>, R> extractor;
    private final List<Consumer<R>> listeners = new ArrayList<>();
    private R value;
    private boolean disposed;

    @SuppressWarnings("unchecked")
    <T> StoreSelector(Function<ElementEntry<T>, R> extractor, ElementEntry<T> initial) {
        this.extractor = entry -> extractor.apply((ElementEntry<T>) entry);
        this.value = initial != null ? extractor.apply(initial) : null;
    }

    void recompute(ElementEntry<?> entry) {
        R next = entry != null ? extractor.apply(entry) : null;
        if (Objects.equals(value, next)) return;
        value = next;
        if (!disposed) listeners.forEach(l -> l.accept(next));
    }

    public R get() {
        return value;
    }

    public void subscribe(Consumer<R> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<R> listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        disposed = true;
        listeners.clear();
    }
}
