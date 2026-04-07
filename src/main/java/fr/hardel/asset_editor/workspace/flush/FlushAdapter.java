package fr.hardel.asset_editor.workspace.flush;

public interface FlushAdapter<T> {

    ElementEntry<T> prepare(ElementEntry<T> entry);

    static <T> FlushAdapter<T> identity() {
        return entry -> entry;
    }
}
