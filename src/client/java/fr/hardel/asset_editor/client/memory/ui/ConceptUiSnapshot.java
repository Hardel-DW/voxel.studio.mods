package fr.hardel.asset_editor.client.memory.ui;

import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.compose.lib.data.StudioViewMode;

import java.util.Set;

public record ConceptUiSnapshot(String search, String filterPath, StudioViewMode viewMode,
    StudioSidebarView sidebarView, Set<String> expandedTreePaths) {

    public ConceptUiSnapshot {
        search = search == null ? "" : search;
        filterPath = filterPath == null ? "" : filterPath;
        viewMode = viewMode == null ? StudioViewMode.LIST : viewMode;
        sidebarView = sidebarView == null ? StudioSidebarView.SLOTS : sidebarView;
        expandedTreePaths = Set.copyOf(expandedTreePaths == null ? Set.of() : expandedTreePaths);
    }

    public ConceptUiSnapshot() {
        this("", "", StudioViewMode.LIST, StudioSidebarView.SLOTS, Set.of());
    }
}
