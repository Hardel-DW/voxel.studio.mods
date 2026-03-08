package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.lib.store.StoreEvent;

public interface EditorPage {
    default void onActivate() {}
    default void onDeactivate() {}
    default void onStoreEvent(StoreEvent event) {}
}
