package fr.hardel.asset_editor.workspace.registry;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegistryMutationDispatcher<T> implements RegistryMutationHandler<T> {

    private final Map<String, BoundHandler<T>> handlers = new ConcurrentHashMap<>();

    public <A extends EditorAction> RegistryMutationDispatcher<T> register(
        EditorActionType<A> type,
        MutationActionHandler<T, A> handler
    ) {
        if (type == null)
            throw new IllegalArgumentException("Action type cannot be null");
        if (handler == null)
            throw new IllegalArgumentException("Mutation handler cannot be null");

        BoundHandler<T> previous = handlers.putIfAbsent(type.id().toString(), new TypedBoundHandler<>(type, handler));
        if (previous != null)
            throw new IllegalStateException("Duplicate mutation handler for action type " + type.id());
        return this;
    }

    @Override
    public void beforeApply(EditorAction action, RegistryMutationContext context) {
        resolve(action.typeId()).beforeApply(action, context);
    }

    @Override
    public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context) {
        return resolve(action.typeId()).apply(entry, action, context);
    }

    private BoundHandler<T> resolve(Identifier actionTypeId) {
        BoundHandler<T> handler = handlers.get(actionTypeId.toString());
        if (handler == null)
            throw new UnsupportedOperationException("No mutation handler registered for action " + actionTypeId);
        return handler;
    }

    private interface BoundHandler<T> {
        void beforeApply(EditorAction action, RegistryMutationContext context);

        ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context);
    }

    private static final class TypedBoundHandler<T, A extends EditorAction> implements BoundHandler<T> {
        private final EditorActionType<A> type;
        private final MutationActionHandler<T, A> handler;

        private TypedBoundHandler(EditorActionType<A> type, MutationActionHandler<T, A> handler) {
            this.type = type;
            this.handler = handler;
        }

        @Override
        public void beforeApply(EditorAction action, RegistryMutationContext context) {
            handler.beforeApply(type.cast(action), context);
        }

        @Override
        public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context) {
            return handler.apply(entry, type.cast(action), context);
        }
    }
}
