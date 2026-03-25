package fr.hardel.asset_editor.client.memory.workspace;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.client.memory.session.SessionMemory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WorkspaceMemory implements ReadableMemory<WorkspaceMemory.Snapshot> {

    public record Snapshot(String worldSessionKey, int pendingActionCount) {

        public Snapshot {
            worldSessionKey = worldSessionKey == null ? "" : worldSessionKey;
        }

        public static Snapshot empty() {
            return new Snapshot("", 0);
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());
    private final PackSelectionMemory packSelection;
    private final IssueMemory issues = new IssueMemory();
    private final RegistryMemory registries = new RegistryMemory();
    private final Map<UUID, PendingClientAction<?>> pendingActions = new LinkedHashMap<>();

    public WorkspaceMemory(SessionMemory sessionMemory) {
        this(sessionMemory, fr.hardel.asset_editor.client.ClientPreferences::lastPackId,
            fr.hardel.asset_editor.client.ClientPreferences::setLastPackId);
    }

    public WorkspaceMemory(SessionMemory sessionMemory, Supplier<String> preferredPackIdSupplier,
        Consumer<String> preferredPackIdConsumer) {
        this.packSelection = new PackSelectionMemory(sessionMemory, preferredPackIdSupplier, preferredPackIdConsumer);
    }

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public PackSelectionMemory packSelection() {
        return packSelection;
    }

    public IssueMemory issues() {
        return issues;
    }

    public RegistryMemory registries() {
        return registries;
    }

    public String worldSessionKey() {
        return snapshot().worldSessionKey();
    }

    public void setWorldSessionKey(String worldSessionKey) {
        memory.update(state -> new Snapshot(worldSessionKey, state.pendingActionCount()));
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

    public List<PendingClientAction<?>> pendingActionsSnapshot() {
        return List.copyOf(pendingActions.values());
    }

    public void resetForWorldSync() {
        registries.clearAll();
        packSelection.clearSelection();
        issues.clear();
        clearPendingActionStateOnly();
    }

    public void resetForWorldClose() {
        memory.setSnapshot(Snapshot.empty());
        resetForWorldSync();
    }

    public void dispose() {
        packSelection.dispose();
    }

    private void clearPendingActionStateOnly() {
        pendingActions.clear();
        syncPendingActionCount();
    }

    private void syncPendingActionCount() {
        memory.update(state -> new Snapshot(state.worldSessionKey(), pendingActions.size()));
    }
}
