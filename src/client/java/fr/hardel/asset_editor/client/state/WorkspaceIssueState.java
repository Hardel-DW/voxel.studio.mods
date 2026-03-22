package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;

import java.util.List;
import java.util.function.Function;

public final class WorkspaceIssueState {

    public record Snapshot(List<String> warnings, List<String> errors) {

        public Snapshot {
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
            errors = List.copyOf(errors == null ? List.of() : errors);
        }

        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of());
        }
    }

    private final MutableSelectorStore<Snapshot> store = new MutableSelectorStore<>(Snapshot.empty());
    public Snapshot snapshot() {
        return store.getState();
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector) {
        return store.select(selector);
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector,
        SelectorEquality<? super R> equality) {
        return store.select(selector, equality);
    }

    public List<String> warnings() {
        return snapshot().warnings();
    }

    public List<String> errors() {
        return snapshot().errors();
    }

    public void replaceWarnings(List<String> nextWarnings) {
        store.update(state -> new Snapshot(nextWarnings, state.errors()));
    }

    public void replaceErrors(List<String> nextErrors) {
        store.update(state -> new Snapshot(state.warnings(), nextErrors));
    }

    public void pushError(String error) {
        if (error == null || error.isBlank())
            return;
        store.update(state -> {
            java.util.ArrayList<String> next = new java.util.ArrayList<>(state.errors());
            next.remove(error);
            next.add(0, error);
            return new Snapshot(state.warnings(), next);
        });
    }

    public void clear() {
        store.setState(Snapshot.empty());
    }

    public void dispose() {
    }
}
