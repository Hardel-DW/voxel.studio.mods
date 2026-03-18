package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.javafx.lib.store.StudioOpenTab;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class WorkspaceTabsState {

    public record Snapshot(List<StudioOpenTab> openTabs, int activeTabIndex, String currentElementId) {

        public Snapshot {
            openTabs = List.copyOf(openTabs == null ? List.of() : openTabs);
            currentElementId = currentElementId == null ? "" : currentElementId;
        }

        public static Snapshot initial() {
            return new Snapshot(List.of(), -1, "");
        }
    }

    private static final int MAX_TABS = 10;

    private final MutableSelectorStore<Snapshot> store = new MutableSelectorStore<>(Snapshot.initial());
    private final ObservableList<StudioOpenTab> openTabs = FXCollections.observableArrayList();
    private final IntegerProperty activeTabIndex = new SimpleIntegerProperty(-1);
    private final SimpleStringProperty currentElementId = new SimpleStringProperty("");
    private final Subscription syncSubscription = store.subscribe(this::syncFromStore);
    private boolean syncing;

    public WorkspaceTabsState() {
        syncFromStore();
    }

    public Snapshot snapshot() {
        return store.getState();
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector) {
        return store.select(selector);
    }

    public ObservableList<StudioOpenTab> openTabs() {
        return openTabs;
    }

    public ReadOnlyIntegerProperty activeTabIndexProperty() {
        return activeTabIndex;
    }

    public ReadOnlyStringProperty currentElementIdProperty() {
        return currentElementId;
    }

    public String currentElementId() {
        return snapshot().currentElementId();
    }

    public void setCurrentElementId(String elementId) {
        store.update(state -> new Snapshot(state.openTabs(), state.activeTabIndex(), elementId));
    }

    public void openElement(String elementId, StudioRoute route) {
        if (elementId == null || elementId.isBlank())
            return;

        store.update(state -> {
            List<StudioOpenTab> nextTabs = new ArrayList<>(state.openTabs());

            for (int i = 0; i < nextTabs.size(); i++) {
                if (nextTabs.get(i).elementId().equals(elementId))
                    return new Snapshot(nextTabs, i, elementId);
            }

            nextTabs.add(new StudioOpenTab(elementId, route));
            if (nextTabs.size() > MAX_TABS)
                nextTabs.removeFirst();
            return new Snapshot(nextTabs, nextTabs.size() - 1, elementId);
        });
    }

    public void switchTab(int index) {
        store.update(state -> {
            if (index < 0 || index >= state.openTabs().size())
                return state;
            return new Snapshot(state.openTabs(), index, state.openTabs().get(index).elementId());
        });
    }

    public void closeTab(int index) {
        store.update(state -> {
            if (index < 0 || index >= state.openTabs().size())
                return state;

            List<StudioOpenTab> nextTabs = new ArrayList<>(state.openTabs());
            nextTabs.remove(index);
            if (nextTabs.isEmpty())
                return Snapshot.initial();

            int nextIndex = state.activeTabIndex();
            if (index < nextIndex)
                nextIndex--;
            if (nextIndex >= nextTabs.size())
                nextIndex = nextTabs.size() - 1;
            return new Snapshot(nextTabs, nextIndex, nextTabs.get(nextIndex).elementId());
        });
    }

    public StudioOpenTab activeTab() {
        Snapshot state = snapshot();
        int index = state.activeTabIndex();
        if (index < 0 || index >= state.openTabs().size())
            return null;
        return state.openTabs().get(index);
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
            if (!openTabs.equals(state.openTabs()))
                openTabs.setAll(state.openTabs());
            if (activeTabIndex.get() != state.activeTabIndex())
                activeTabIndex.set(state.activeTabIndex());
            if (!currentElementId.get().equals(state.currentElementId()))
                currentElementId.set(state.currentElementId());
        } finally {
            syncing = false;
        }
    }
}
