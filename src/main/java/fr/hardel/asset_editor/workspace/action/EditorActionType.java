package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class EditorActionType<T, A extends EditorAction> {

    private final Identifier id;
    private final Class<A> javaType;
    private final StreamCodec<ByteBuf, A> codec;
    private final ActionHandler<T, A> handler;

    public EditorActionType(Identifier id, Class<A> javaType, StreamCodec<ByteBuf, A> codec, ActionHandler<T, A> handler) {
        if (id == null)
            throw new IllegalArgumentException("Action id cannot be null");
        if (javaType == null)
            throw new IllegalArgumentException("Action javaType cannot be null");
        if (codec == null)
            throw new IllegalArgumentException("Action codec cannot be null");
        if (handler == null)
            throw new IllegalArgumentException("Action handler cannot be null");

        this.id = id;
        this.javaType = javaType;
        this.codec = codec;
        this.handler = handler;
    }

    public Identifier id() {
        return id;
    }

    public void encode(ByteBuf buffer, EditorAction action) {
        codec.encode(buffer, cast(action));
    }

    public A decode(ByteBuf buffer) {
        return codec.decode(buffer);
    }

    public A cast(EditorAction action) {
        try {
            return javaType.cast(action);
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException("Action " + action + " is not compatible with type " + id, exception);
        }
    }

    public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction rawAction, RegistryMutationContext ctx) {
        return handler.apply(entry, cast(rawAction), ctx);
    }

    @SuppressWarnings("unchecked") // Safe: actions are registered per workspace, T always matches
    public static <T> ElementEntry<T> dispatch(ElementEntry<T> entry, EditorAction action, RegistryMutationContext ctx) {
        EditorActionType<T, ?> type = (EditorActionType<T, ?>) action.type();
        return type.apply(entry, action, ctx);
    }
}
