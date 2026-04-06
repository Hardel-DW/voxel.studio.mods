package fr.hardel.asset_editor.workspace.action;

import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class EditorActionRegistry {

    public static final ResourceKey<Registry<EditorActionType<?, ?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "editor_action_type"));

    public static final Registry<EditorActionType<?, ?>> REGISTRY = new MappedRegistry<>(REGISTRY_KEY, Lifecycle.stable());

    private static final StreamCodec<ByteBuf, EditorAction> STREAM_CODEC = StreamCodec.of(
        (buffer, action) -> {
            EditorActionType<?, ? extends EditorAction> type = action.type();
            Identifier.STREAM_CODEC.encode(buffer, type.id());
            type.encode(buffer, action);
        },
        buffer -> {
            Identifier id = Identifier.STREAM_CODEC.decode(buffer);
            EditorActionType<?, ?> type = get(id);
            if (type == null)
                throw new IllegalArgumentException("Unknown action type: " + id);
            return type.decode(buffer);
        }
    );

    public static <T, A extends EditorAction> EditorActionType<T, A> register(EditorActionType<T, A> type) {
        if (type == null)
            throw new IllegalArgumentException("Action type cannot be null");
        return Registry.register(REGISTRY, type.id(), type);
    }

    public static EditorActionType<?, ?> get(Identifier id) {
        if (id == null)
            return null;
        freeze();
        return REGISTRY.getValue(id);
    }

    public static StreamCodec<ByteBuf, EditorAction> streamCodec() {
        return STREAM_CODEC;
    }

    public static void freeze() {
        REGISTRY.freeze();
    }

    private EditorActionRegistry() {
    }
}
