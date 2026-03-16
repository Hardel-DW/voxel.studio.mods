package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.store.ElementEntry;

public interface RegistryInterpreter<T> {
    ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action);
}
