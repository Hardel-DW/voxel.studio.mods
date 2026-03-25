package fr.hardel.asset_editor.client.memory.core;

import java.util.function.UnaryOperator;

public interface MutableMemory<S> extends ReadableMemory<S> {

    void setSnapshot(S nextSnapshot);

    S update(UnaryOperator<S> updater);
}
