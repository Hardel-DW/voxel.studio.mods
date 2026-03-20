package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class ClientWorkspaceState {

    public record Snapshot(String worldSessionKey, int pendingActionCount) {

        public Snapshot {
            worldSessionKey = worldSessionKey == null ? "" : worldSessionKey;
        }

        public static Snapshot empty() {
            return new Snapshot("", 0);
        }
    }

    private final MutableSelectorStore<Snapshot> store = new MutableSelectorStore<>(Snapshot.empty());
    private final WorkspaceUiState uiState = new WorkspaceUiState();
    private final WorkspaceTabsState tabsState = new WorkspaceTabsState();
    private final WorkspacePackSelectionState packSelectionState;
    private final WorkspaceIssueState issueState = new WorkspaceIssueState();
    private final RegistryElementStore elementStore = new RegistryElementStore();
    private final Map<UUID, PendingClientAction<?>> pendingActions = new LinkedHashMap<>();

    public ClientWorkspaceState(ClientSessionState sessionState) {
        this.packSelectionState = new WorkspacePackSelectionState(sessionState);
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

    public WorkspaceUiState uiState() {
        return uiState;
    }

    public WorkspaceTabsState tabsState() {
        return tabsState;
    }

    public WorkspacePackSelectionState packSelectionState() {
        return packSelectionState;
    }

    public WorkspaceIssueState issueState() {
        return issueState;
    }

    public RegistryElementStore elementStore() {
        return elementStore;
    }

    public String worldSessionKey() {
        return snapshot().worldSessionKey();
    }

    public void setWorldSessionKey(String worldSessionKey) {
        store.update(state -> new Snapshot(worldSessionKey, state.pendingActionCount()));
    }

    public void trackPendingAction(UUID actionId, PendingClientAction<?> action) {
        pendingActions.put(actionId, action);
        syncPendingActionCount();
    }

    public PendingClientAction<?> removePendingAction(UUID actionId) {
        PendingClientAction<?> removed = pendingActions.remove(actionId);
        if (removed != null)
            syncPendingActionCount();
        return removed;
    }

    public void clearPendingActions() {
        pendingActions.clear();
        syncPendingActionCount();
    }

    public java.util.List<PendingClientAction<?>> pendingActionsSnapshot() {
        return java.util.List.copyOf(pendingActions.values());
    }

    public void resetForWorldSync() {
        elementStore.clearAll();
        tabsState.reset();
        packSelectionState.clearSelection();
        issueState.clear();
        clearPendingActionStateOnly();
        uiState.reset();
    }

    public void resetForWorldClose() {
        store.setState(Snapshot.empty());
        resetForWorldSync();
    }

    public void dispose() {
        uiState.dispose();
        tabsState.dispose();
        packSelectionState.dispose();
        issueState.dispose();
    }

    private void clearPendingActionStateOnly() {
        pendingActions.clear();
        syncPendingActionCount();
    }

    private void syncPendingActionCount() {
        store.update(state -> new Snapshot(state.worldSessionKey(), pendingActions.size()));
    }
}
