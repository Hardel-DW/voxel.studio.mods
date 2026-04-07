package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class Action<A extends EditorAction<?>> {

    private final Identifier id;
    private final Class<A> actionClass;
    private final StreamCodec<ByteBuf, A> codec;

    public Action(Identifier id, Class<A> actionClass, StreamCodec<ByteBuf, A> codec) {
        this.id = id;
        this.actionClass = actionClass;
        this.codec = codec;
    }

    public Identifier id() {
        return id;
    }

    public void encode(ByteBuf buffer, EditorAction<?> action) {
        codec.encode(buffer, actionClass.cast(action));
    }

    public A decode(ByteBuf buffer) {
        return codec.decode(buffer);
    }

    @SuppressWarnings("unchecked") // Safe: actions are registered per workspace, T always matches
    public static <T> ElementEntry<T> dispatch(ElementEntry<T> entry, EditorAction<?> action, RegistryMutationContext ctx) {
        return ((EditorAction<T>) action).apply(entry, ctx);
    }
}
