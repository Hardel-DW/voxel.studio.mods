package fr.hardel.asset_editor.client.selector;

@FunctionalInterface
public interface Subscription {

    void unsubscribe();
}
