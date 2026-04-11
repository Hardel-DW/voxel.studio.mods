package fr.hardel.asset_editor.workspace.flush;

import com.google.gson.JsonElement;

public interface FlushAdapter<T> {

    ElementEntry<T> prepare(ElementEntry<T> entry);

    default JsonElement postEncode(JsonElement encoded) {
        return encoded;
    }

    static <T> FlushAdapter<T> identity() {
        return entry -> entry;
    }
}
