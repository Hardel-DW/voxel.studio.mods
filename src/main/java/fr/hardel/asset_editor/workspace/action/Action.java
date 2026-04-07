package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class Action<T, A extends EditorAction<T>> {

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

    public Class<A> actionClass() {
        return actionClass;
    }

    public void encode(ByteBuf buffer, EditorAction<?> action) {
        codec.encode(buffer, actionClass.cast(action));
    }

    public A decode(ByteBuf buffer) {
        return codec.decode(buffer);
    }

    public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction<?> action, RegistryMutationContext ctx) {
        return actionClass.cast(action).apply(entry, ctx);
    }
}
