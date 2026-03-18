package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
    private final ObservableList<String> warnings = FXCollections.observableArrayList();
    private final ObservableList<String> errors = FXCollections.observableArrayList();
    private final Subscription syncSubscription = store.subscribe(this::syncFromStore);
    private boolean syncing;

    public WorkspaceIssueState() {
        syncFromStore();
    }

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

    public ObservableList<String> warnings() {
        return warnings;
    }

    public ObservableList<String> errors() {
        return errors;
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
        syncSubscription.unsubscribe();
    }

    private void syncFromStore() {
        if (syncing)
            return;

        syncing = true;
        try {
            Snapshot state = snapshot();
            if (!warnings.equals(state.warnings()))
                warnings.setAll(state.warnings());
            if (!errors.equals(state.errors()))
                errors.setAll(state.errors());
        } finally {
            syncing = false;
        }
    }
}
