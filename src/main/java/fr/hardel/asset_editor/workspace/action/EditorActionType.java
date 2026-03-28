package fr.hardel.asset_editor.workspace.action;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class EditorActionType<T extends EditorAction> {

    private final Identifier id;
    private final Class<T> javaType;
    private final StreamCodec<ByteBuf, T> codec;

    public EditorActionType(Identifier id, Class<T> javaType, StreamCodec<ByteBuf, T> codec) {
        if (id == null)
            throw new IllegalArgumentException("Action id cannot be null");
        if (javaType == null)
            throw new IllegalArgumentException("Action javaType cannot be null");
        if (codec == null)
            throw new IllegalArgumentException("Action codec cannot be null");

        this.id = id;
        this.javaType = javaType;
        this.codec = codec;
    }

    public Identifier id() {
        return id;
    }

    public void encode(ByteBuf buffer, EditorAction action) {
        codec.encode(buffer, cast(action));
    }

    public T decode(ByteBuf buffer) {
        return codec.decode(buffer);
    }

    public T cast(EditorAction action) {
        try {
            return javaType.cast(action);
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException("Action " + action + " is not compatible with type " + id, exception);
        }
    }
}
