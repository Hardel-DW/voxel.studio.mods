package fr.hardel.asset_editor.client.selector;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StoreSelection<S, R> {

    private final SelectorStore<S> store;
    private final Function<? super S, ? extends R> selector;
    private final SelectorEquality<? super R> equality;

    StoreSelection(SelectorStore<S> store,
        Function<? super S, ? extends R> selector,
        SelectorEquality<? super R> equality) {
        this.store = store;
        this.selector = selector;
        this.equality = equality;
    }

    public R get() {
        return selector.apply(store.getState());
    }

    public Subscription subscribe(Consumer<? super R> listener) {
        return subscribe(listener, false);
    }

    public Subscription subscribe(Consumer<? super R> listener, boolean emitCurrent) {
        Objects.requireNonNull(listener, "listener");

        AtomicReference<R> current = new AtomicReference<>(get());
        if (emitCurrent)
            listener.accept(current.get());

        return store.subscribe(() -> {
            R previous = current.get();
            R next = get();
            if (equality.same(previous, next))
                return;
            current.set(next);
            listener.accept(next);
        });
    }
}
