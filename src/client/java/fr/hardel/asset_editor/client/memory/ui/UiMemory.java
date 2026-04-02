package fr.hardel.asset_editor.client.memory.ui;

import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import net.minecraft.core.registries.Registries;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class UiMemory implements ReadableMemory<UiMemory.Snapshot> {

    public record Snapshot(Map<StudioConcept, ConceptUiSnapshot> concepts) {

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

    public ConceptUiSnapshot conceptSnapshot(StudioConcept concept) {
        return snapshot().concepts().getOrDefault(concept, defaultSnapshot(concept));
    }

    public void updateSearch(StudioConcept concept, String value) {
        updateConcept(concept, current -> new ConceptUiSnapshot(value, current.filterPath(), current.sidebarView(), current.expandedTreePaths()));
    }

    public void updateFilterPath(StudioConcept concept, String value) {
        updateConcept(concept, current -> new ConceptUiSnapshot(current.search(), value, current.sidebarView(), current.expandedTreePaths()));
    }

    public void updateSidebarView(StudioConcept concept, StudioSidebarView value) {
        updateConcept(concept, current -> new ConceptUiSnapshot(current.search(), "", value, Set.of()));
    }

    public void setTreeExpanded(StudioConcept concept, String path, boolean expanded) {
        updateConcept(concept, current -> {
            LinkedHashSet<String> next = new LinkedHashSet<>(current.expandedTreePaths());
            if (expanded) {
                next.add(path);
            } else {
                next.remove(path);
            }

            return new ConceptUiSnapshot(current.search(), current.filterPath(), current.sidebarView(), next);
        });
    }

    public void resetConcept(StudioConcept concept) {
        memory.update(state -> {
            if (!state.concepts().containsKey(concept))
                return state;

            LinkedHashMap<StudioConcept, ConceptUiSnapshot> next = new LinkedHashMap<>(state.concepts());
            next.remove(concept);
            return new Snapshot(next);
        });
    }

    public void reset() {
        memory.setSnapshot(Snapshot.empty());
    }

    private void updateConcept(StudioConcept concept, Function<ConceptUiSnapshot, ConceptUiSnapshot> updater) {
        memory.update(state -> {
            ConceptUiSnapshot current = state.concepts().getOrDefault(concept, defaultSnapshot(concept));
            ConceptUiSnapshot next = updater.apply(current);
            if (next.equals(current))
                return state;

            LinkedHashMap<StudioConcept, ConceptUiSnapshot> concepts = new LinkedHashMap<>(state.concepts());
            concepts.put(concept, next);
            return new Snapshot(concepts);
        });
    }

    private ConceptUiSnapshot defaultSnapshot(StudioConcept concept) {
        if (concept.getRegistryKey().equals(Registries.ENCHANTMENT))
            return new ConceptUiSnapshot("", "", StudioSidebarView.SLOTS, Set.of());

        return new ConceptUiSnapshot();
    }
}
