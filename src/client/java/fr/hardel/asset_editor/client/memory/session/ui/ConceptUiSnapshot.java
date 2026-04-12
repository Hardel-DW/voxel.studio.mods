package fr.hardel.asset_editor.client.memory.session.ui;

import fr.hardel.asset_editor.client.compose.components.page.enchantment.StudioSidebarView;

import java.util.Map;

public record ConceptUiSnapshot(String search, String filterPath,
    StudioSidebarView sidebarView, Map<String, Boolean> treeExpansion) {

    public ConceptUiSnapshot {
        search = search == null ? "" : search;
        filterPath = filterPath == null ? "" : filterPath;
        sidebarView = sidebarView == null ? StudioSidebarView.SLOTS : sidebarView;
        treeExpansion = Map.copyOf(treeExpansion == null ? Map.of() : treeExpansion);
    }

    public ConceptUiSnapshot() {
        this("", "", StudioSidebarView.SLOTS, Map.of());
    }
}
