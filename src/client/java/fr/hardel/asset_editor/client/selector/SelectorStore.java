package fr.hardel.asset_editor.client.selector;

import java.util.Objects;
import java.util.function.Function;

public interface SelectorStore<S> {

    S getState();

    Subscription subscribe(Runnable listener);

    default <R> StoreSelection<S, R> select(Function<? super S, ? extends R> selector) {
        return select(selector, SelectorEquality.equalsEquality());
    }

    default <R> StoreSelection<S, R> select(Function<? super S, ? extends R> selector,
                                            SelectorEquality<? super R> equality) {
        Objects.requireNonNull(selector, "selector");
        Objects.requireNonNull(equality, "equality");
        return new StoreSelection<>(this, selector, equality);
    }
}
