package fr.hardel.asset_editor.workspace.registry;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.EditorAction;

public interface MutationActionHandler<T, A extends EditorAction> {

    default void beforeApply(A action, RegistryMutationContext context) {
    }

    ElementEntry<T> apply(ElementEntry<T> entry, A action, RegistryMutationContext context);

    static <T, A extends EditorAction> MutationActionHandler<T, A> of(ActionApplier<T, A> applier) {
        return new MutationActionHandler<>() {
            @Override
            public ElementEntry<T> apply(ElementEntry<T> entry, A action, RegistryMutationContext context) {
                return applier.apply(entry, action, context);
            }
        };
    }

    @FunctionalInterface
    interface ActionApplier<T, A extends EditorAction> {
        ElementEntry<T> apply(ElementEntry<T> entry, A action, RegistryMutationContext context);
    }
}
