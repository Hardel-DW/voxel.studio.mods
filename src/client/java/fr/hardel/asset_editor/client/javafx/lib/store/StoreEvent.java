package fr.hardel.asset_editor.client.javafx.lib.store;

import net.minecraft.resources.Identifier;

public sealed interface StoreEvent {
    record ElementChanged(String registry, Identifier id) implements StoreEvent {}
    record TagToggled(String registry, Identifier elementId, Identifier tagId) implements StoreEvent {}
}
