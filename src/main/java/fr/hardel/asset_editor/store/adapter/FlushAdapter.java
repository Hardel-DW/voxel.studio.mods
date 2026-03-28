package fr.hardel.asset_editor.store.adapter;

import fr.hardel.asset_editor.store.ElementEntry;

public interface FlushAdapter<T> {

    ElementEntry<T> prepare(ElementEntry<T> entry);

    static <T> FlushAdapter<T> identity() {
        return entry -> entry;
    }
}
