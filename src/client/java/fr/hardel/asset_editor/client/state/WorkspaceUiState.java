package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode;
import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.function.Function;

public final class WorkspaceUiState {

    public record Snapshot(String search, String filterPath, StudioViewMode viewMode, StudioSidebarView sidebarView) {

        public Snapshot {
            search = search == null ? "" : search;
            filterPath = filterPath == null ? "" : filterPath;
            viewMode = viewMode == null ? StudioViewMode.LIST : viewMode;
            sidebarView = sidebarView == null ? StudioSidebarView.SLOTS : sidebarView;
        }

        public static Snapshot initial() {
            return new Snapshot("", "", StudioViewMode.LIST, StudioSidebarView.SLOTS);
        }
    }

    private final MutableSelectorStore<Snapshot> store = new MutableSelectorStore<>(Snapshot.initial());
    private final StringProperty search = new SimpleStringProperty("");
    private final StringProperty filterPath = new SimpleStringProperty("");
    private final ObjectProperty<StudioViewMode> viewMode = new SimpleObjectProperty<>(StudioViewMode.LIST);
    private final ObjectProperty<StudioSidebarView> sidebarView = new SimpleObjectProperty<>(StudioSidebarView.SLOTS);
    private final Subscription syncSubscription = store.subscribe(this::syncFromStore);
    private boolean syncing;

    public WorkspaceUiState() {
        search.addListener((obs, oldValue, newValue) -> {
            if (!syncing)
                setSearch(newValue);
        });
        filterPath.addListener((obs, oldValue, newValue) -> {
            if (!syncing)
                setFilterPath(newValue);
        });
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

    public StringProperty searchProperty() {
        return search;
    }

    public StringProperty filterPathProperty() {
        return filterPath;
    }

    public ReadOnlyObjectProperty<StudioViewMode> viewModeProperty() {
        return viewMode;
    }

    public ReadOnlyObjectProperty<StudioSidebarView> sidebarViewProperty() {
        return sidebarView;
    }

    public String search() {
        return snapshot().search();
    }

    public String filterPath() {
        return snapshot().filterPath();
    }

    public StudioViewMode viewMode() {
        return snapshot().viewMode();
    }

    public StudioSidebarView sidebarView() {
        return snapshot().sidebarView();
    }

    public void setSearch(String value) {
        store.update(state -> new Snapshot(value, state.filterPath(), state.viewMode(), state.sidebarView()));
    }

    public void setFilterPath(String value) {
        store.update(state -> new Snapshot(state.search(), value, state.viewMode(), state.sidebarView()));
    }

    public void setViewMode(StudioViewMode mode) {
        if (mode == null)
            return;
        store.update(state -> new Snapshot(state.search(), state.filterPath(), mode, state.sidebarView()));
    }

    public void setSidebarView(StudioSidebarView mode) {
        if (mode == null)
            return;
        store.update(state -> new Snapshot(state.search(), "", state.viewMode(), mode));
    }

    public void reset() {
        store.setState(Snapshot.initial());
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
            if (!search.get().equals(state.search()))
                search.set(state.search());

            if (!filterPath.get().equals(state.filterPath()))
                filterPath.set(state.filterPath());

            if (viewMode.get() != state.viewMode())
                viewMode.set(state.viewMode());

            if (sidebarView.get() != state.sidebarView())
                sidebarView.set(state.sidebarView());
        } finally {
            syncing = false;
        }
    }
}
