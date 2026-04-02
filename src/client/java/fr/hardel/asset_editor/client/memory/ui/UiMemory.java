package fr.hardel.asset_editor.client.memory.ui;

import fr.hardel.asset_editor.studio.StudioUiRegistry;
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class UiMemory implements ReadableMemory<UiMemory.Snapshot> {

    public record Snapshot(Map<Identifier, ConceptUiSnapshot> concepts) {

        public Snapshot {
            concepts = Map.copyOf(concepts == null ? Map.of() : concepts);
        }

        public static Snapshot empty() {
            return new Snapshot(Map.of());
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public ConceptUiSnapshot conceptSnapshot(Identifier conceptId) {
        return snapshot().concepts().getOrDefault(conceptId, defaultSnapshot(conceptId));
    }

    public void updateSearch(Identifier conceptId, String value) {
        updateConcept(conceptId, current -> new ConceptUiSnapshot(value, current.filterPath(), current.sidebarView(), current.expandedTreePaths()));
    }

    public void updateFilterPath(Identifier conceptId, String value) {
        updateConcept(conceptId, current -> new ConceptUiSnapshot(current.search(), value, current.sidebarView(), current.expandedTreePaths()));
    }

    public void updateSidebarView(Identifier conceptId, StudioSidebarView value) {
        updateConcept(conceptId, current -> new ConceptUiSnapshot(current.search(), "", value, Set.of()));
    }

    public void setTreeExpanded(Identifier conceptId, String path, boolean expanded) {
        updateConcept(conceptId, current -> {
            LinkedHashSet<String> next = new LinkedHashSet<>(current.expandedTreePaths());
            if (expanded) {
                next.add(path);
            } else {
                next.remove(path);
            }

            return new ConceptUiSnapshot(current.search(), current.filterPath(), current.sidebarView(), next);
        });
    }

    public void resetConcept(Identifier conceptId) {
        memory.update(state -> {
            if (!state.concepts().containsKey(conceptId))
                return state;

            LinkedHashMap<Identifier, ConceptUiSnapshot> next = new LinkedHashMap<>(state.concepts());
            next.remove(conceptId);
            return new Snapshot(next);
        });
    }

    public void reset() {
        memory.setSnapshot(Snapshot.empty());
    }

    private void updateConcept(Identifier conceptId, Function<ConceptUiSnapshot, ConceptUiSnapshot> updater) {
        memory.update(state -> {
            ConceptUiSnapshot current = state.concepts().getOrDefault(conceptId, defaultSnapshot(conceptId));
            ConceptUiSnapshot next = updater.apply(current);
            if (next.equals(current))
                return state;

            LinkedHashMap<Identifier, ConceptUiSnapshot> concepts = new LinkedHashMap<>(state.concepts());
            concepts.put(conceptId, next);
            return new Snapshot(concepts);
        });
    }

    private ConceptUiSnapshot defaultSnapshot(Identifier conceptId) {
        return StudioUiRegistry.defaultSnapshot(conceptId);
    }
}
