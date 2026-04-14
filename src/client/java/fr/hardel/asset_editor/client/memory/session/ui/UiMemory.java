package fr.hardel.asset_editor.client.memory.session.ui;

import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry;
import fr.hardel.asset_editor.client.compose.components.page.enchantment.StudioSidebarView;
import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
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
        updateConcept(conceptId, current -> new ConceptUiSnapshot(value, current.filterPath(), current.sidebarView(), current.treeExpansion(), current.showAll()));
    }

    public void updateFilterPath(Identifier conceptId, String value) {
        updateConcept(conceptId, current -> new ConceptUiSnapshot(current.search(), value, current.sidebarView(), current.treeExpansion(), current.showAll()));
    }

    public void updateSidebarView(Identifier conceptId, StudioSidebarView value) {
        updateConcept(conceptId, current -> new ConceptUiSnapshot(current.search(), "", value, Map.of(), current.showAll()));
    }

    public void setTreeExpanded(Identifier conceptId, String path, boolean expanded) {
        updateConcept(conceptId, current -> {
            LinkedHashMap<String, Boolean> next = new LinkedHashMap<>(current.treeExpansion());
            next.put(path, expanded);
            return new ConceptUiSnapshot(current.search(), current.filterPath(), current.sidebarView(), next, current.showAll());
        });
    }

    public void setShowAll(Identifier conceptId, boolean showAll) {
        updateConcept(conceptId, current -> new ConceptUiSnapshot(current.search(), current.filterPath(), current.sidebarView(), current.treeExpansion(), showAll));
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
