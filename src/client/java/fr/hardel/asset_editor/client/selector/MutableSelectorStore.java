package fr.hardel.asset_editor.client.selector;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

public final class MutableSelectorStore<S> implements SelectorStore<S> {

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile S state;

    public MutableSelectorStore(S initialState) {
        this.state = initialState;
    }

    @Override
    public S getState() {
        return state;
    }

    public void setState(S nextState) {
        synchronized (this) {
            if (Objects.equals(state, nextState))
                return;
            state = nextState;
        }
        listeners.forEach(Runnable::run);
    }

    public S update(UnaryOperator<S> updater) {
        Objects.requireNonNull(updater, "updater");

        S previous;
        S nextState;
        synchronized (this) {
            previous = state;
            nextState = updater.apply(previous);
            if (Objects.equals(previous, nextState))
                return previous;
            state = nextState;
        }
        listeners.forEach(Runnable::run);
        return nextState;
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
