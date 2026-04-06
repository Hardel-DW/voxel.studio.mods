package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;

@FunctionalInterface
public interface ActionHandler<T, A extends EditorAction> {

    ElementEntry<T> apply(ElementEntry<T> entry, A action, RegistryMutationContext context);
}
