package fr.hardel.asset_editor.workspace.definition;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContext;

@FunctionalInterface
public interface ActionHandler<T, A extends EditorAction> {

    ElementEntry<T> apply(ElementEntry<T> entry, A action, RegistryMutationContext context);
}
