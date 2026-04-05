package fr.hardel.asset_editor.client.memory.ui;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.client.compose.lib.ConceptChangesDestination;
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination;
import fr.hardel.asset_editor.client.compose.lib.DebugDestination;
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination;
import fr.hardel.asset_editor.client.compose.lib.NoPermissionDestination;
import fr.hardel.asset_editor.client.compose.lib.StudioDestination;
import fr.hardel.asset_editor.client.compose.lib.StudioTabEntry;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class NavigationMemory implements ReadableMemory<NavigationMemory.Snapshot> {

    public record Snapshot(StudioDestination current, List<StudioTabEntry> tabs, String activeTabId) {

        public Snapshot {
            current = current == null ? NoPermissionDestination.INSTANCE : current;
            tabs = List.copyOf(tabs == null ? List.of() : tabs);
        }

        public static Snapshot empty() {
            return new Snapshot(NoPermissionDestination.INSTANCE, List.of(), null);
        }
    }

    private final Supplier<StudioPermissions> permissionSupplier;
    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());

    public NavigationMemory(Supplier<StudioPermissions> permissionSupplier) {
        this.permissionSupplier = permissionSupplier;
    }

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public void navigate(StudioDestination destination) {
        StudioDestination normalized = normalize(destination);
        memory.update(state -> new Snapshot(normalized, state.tabs(), state.activeTabId()));
    }

    public void openElement(ElementEditorDestination destination) {
        StudioDestination normalized = normalize(destination);
        if (!(normalized instanceof ElementEditorDestination editorDestination)) {
            navigate(normalized);
            return;
        }

        String tabId = tabIdOf(editorDestination);
        memory.update(state -> {
            ArrayList<StudioTabEntry> nextTabs = new ArrayList<>(state.tabs());
            StudioTabEntry nextEntry = new StudioTabEntry(tabId, editorDestination);
            int existingIndex = nextTabs.stream().filter(tab -> tab.getTabId().equals(tabId)).findFirst().map(nextTabs::indexOf).orElse(-1);

            if (existingIndex >= 0) {
                nextTabs.set(existingIndex, nextEntry);
            } else {
                nextTabs.add(nextEntry);
            }

            return new Snapshot(editorDestination, nextTabs, tabId);
        });
    }

    public void switchTab(String tabId) {
        memory.update(state -> {
            StudioTabEntry entry = state.tabs().stream().filter(tab -> tab.getTabId().equals(tabId)).findFirst().orElse(null);
            if (entry == null)
                return state;

            return new Snapshot(entry.getDestination(), state.tabs(), entry.getTabId());
        });
    }

    public void closeTab(String tabId) {
        memory.update(state -> {
            List<StudioTabEntry> currentTabs = state.tabs();
            int index = currentTabs.stream()
                .filter(tab -> tab.getTabId().equals(tabId))
                .findFirst()
                .map(currentTabs::indexOf)
                .orElse(-1);

            if (index < 0) {
                return state;
            }

            ArrayList<StudioTabEntry> nextTabs = new ArrayList<>(currentTabs);
            nextTabs.remove(index);
            if (nextTabs.isEmpty()) {
                StudioDestination fallback = overviewFallback(state.current());
                return new Snapshot(normalize(fallback), List.of(), null);
            }

            String nextActiveId = !tabId.equals(state.activeTabId())
                ? state.activeTabId()
                : (index >= nextTabs.size()
                    ? nextTabs.getLast().getTabId()
                    : nextTabs.get(index).getTabId());

            StudioDestination nextCurrent = nextTabs.stream()
                .filter(entry -> entry.getTabId().equals(nextActiveId))
                .findFirst()
                .<StudioDestination> map(StudioTabEntry::getDestination)
                .orElse(NoPermissionDestination.INSTANCE);

            return new Snapshot(nextCurrent, nextTabs, nextActiveId);
        });
    }

    public void replaceCurrentTab(ElementEditorDestination destination) {
        StudioDestination normalized = normalize(destination);
        if (!(normalized instanceof ElementEditorDestination editorDestination)) {
            navigate(normalized);
            return;
        }

        memory.update(state -> {
            String activeId = state.activeTabId();
            if (activeId == null) {
                String tabId = tabIdOf(editorDestination);
                ArrayList<StudioTabEntry> nextTabs = new ArrayList<>(state.tabs());
                nextTabs.add(new StudioTabEntry(tabId, editorDestination));
                return new Snapshot(editorDestination, nextTabs, tabId);
            }

            List<StudioTabEntry> nextTabs = state.tabs().stream()
                .map(entry -> entry.getTabId().equals(activeId) ? new StudioTabEntry(activeId, editorDestination) : entry)
                .toList();

            return new Snapshot(editorDestination, nextTabs, activeId);
        });
    }

    public void revalidate(StudioPermissions permissions) {
        memory.update(state -> {
            StudioDestination normalized = normalize(state.current(), permissions);
            if (normalized.equals(state.current()))
                return state;

            if (normalized == NoPermissionDestination.INSTANCE) {
                return Snapshot.empty();
            }

            return new Snapshot(normalized, state.tabs(), state.activeTabId());
        });
    }

    public void reset() {
        memory.setSnapshot(Snapshot.empty());
    }

    public StudioTabEntry activeTab() {
        String activeId = snapshot().activeTabId();
        if (activeId == null)
            return null;

        return snapshot().tabs().stream().filter(entry -> entry.getTabId().equals(activeId)).findFirst().orElse(null);
    }

    private StudioDestination normalize(StudioDestination destination) {
        return normalize(destination, permissionSupplier.get());
    }

    private StudioDestination normalize(StudioDestination destination, StudioPermissions permissions) {
        if (destination == NoPermissionDestination.INSTANCE)
            return destination;

        if (permissions.isNone())
            return NoPermissionDestination.INSTANCE;

        if (destination == DebugDestination.INSTANCE && !permissions.isAdmin())
            return NoPermissionDestination.INSTANCE;

        return destination;
    }

    private StudioDestination overviewFallback(StudioDestination destination) {
        if (destination instanceof ElementEditorDestination editorDestination)
            return new ConceptOverviewDestination(editorDestination.getConceptId());

        if (destination instanceof ConceptChangesDestination changesDestination)
            return new ConceptOverviewDestination(changesDestination.getConceptId());

        if (permissionSupplier.get().isNone())
            return NoPermissionDestination.INSTANCE;

        Identifier conceptId = StudioUiRegistry.firstSupportedConceptId();
        return conceptId != null ? new ConceptOverviewDestination(conceptId) : NoPermissionDestination.INSTANCE;
    }

    private String tabIdOf(ElementEditorDestination destination) {
        return destination.getConceptId() + ":" + destination.getElementId();
    }
}
