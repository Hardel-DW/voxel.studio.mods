package fr.hardel.asset_editor.workspace.registry;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.EditorAction;

public interface RegistryMutationHandler<T> {

    default void beforeApply(EditorAction action, RegistryMutationContext context) {
    }

    ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context);

    static <T> RegistryMutationDispatcher<T> dispatcher() {
        return new RegistryMutationDispatcher<>();
    }

    static <T> RegistryMutationHandler<T> unsupported() {
        return new RegistryMutationHandler<>() {
            @Override
            public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context) {
                throw new UnsupportedOperationException("No mutation handler registered for action " + action.typeId());
            }
        };
    }
}
