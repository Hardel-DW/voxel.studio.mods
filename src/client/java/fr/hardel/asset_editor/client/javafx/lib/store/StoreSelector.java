package fr.hardel.asset_editor.client.javafx.lib.store;

import fr.hardel.asset_editor.client.selector.SelectorStore;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import fr.hardel.asset_editor.store.ElementEntry;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StoreSelector<R> {

    private final StoreSelection<ElementEntry<?>, R> selection;
    private final Map<Consumer<R>, Subscription> subscriptions = new IdentityHashMap<>();
    private boolean disposed;

    @SuppressWarnings("unchecked")
    <T> StoreSelector(SelectorStore<ElementEntry<?>> store, Function<ElementEntry<T>, R> extractor) {
        this.selection = store.select(entry -> entry == null ? null : extractor.apply((ElementEntry<T>) entry));
    }

    public R get() {
        return selection.get();
    }

    public void subscribe(Consumer<R> listener) {
        if (disposed)
            return;
        unsubscribe(listener);
        subscriptions.put(listener, selection.subscribe(listener));
    }

    public void unsubscribe(Consumer<R> listener) {
        Subscription subscription = subscriptions.remove(listener);
        if (subscription != null)
            subscription.unsubscribe();
    }

    public void dispose() {
        if (disposed)
            return;
        disposed = true;
        subscriptions.values().forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }
}
