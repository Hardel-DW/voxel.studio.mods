package fr.hardel.asset_editor.client.memory.ui;

import fr.hardel.asset_editor.client.compose.components.page.enchantment.StudioSidebarView;

import java.util.Set;

public record ConceptUiSnapshot(String search, String filterPath,
    StudioSidebarView sidebarView, Set<String> expandedTreePaths) {

    public ConceptUiSnapshot {
        search = search == null ? "" : search;
        filterPath = filterPath == null ? "" : filterPath;
        sidebarView = sidebarView == null ? StudioSidebarView.SLOTS : sidebarView;
        expandedTreePaths = Set.copyOf(expandedTreePaths == null ? Set.of() : expandedTreePaths);
    }

    public ConceptUiSnapshot() {
        this("", "", StudioSidebarView.SLOTS, Set.of());
    }
}
