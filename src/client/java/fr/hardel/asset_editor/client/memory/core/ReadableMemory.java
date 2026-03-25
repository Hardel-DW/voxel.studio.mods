package fr.hardel.asset_editor.client.memory.core;

public interface ReadableMemory<S> {

    S snapshot();

    Subscription subscribe(Runnable listener);
}
