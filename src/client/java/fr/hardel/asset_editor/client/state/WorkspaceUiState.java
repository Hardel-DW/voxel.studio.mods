package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.compose.lib.data.StudioViewMode;
import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;

import fr.hardel.asset_editor.client.selector.Subscription;
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

    public Snapshot snapshot() {
        return store.getState();
    }

    public Subscription subscribe(Runnable listener) {
        return store.subscribe(listener);
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector) {
        return store.select(selector);
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector,
        SelectorEquality<? super R> equality) {
        return store.select(selector, equality);
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
    }
}
