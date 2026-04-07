package fr.hardel.asset_editor.workspace.action;

import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorActionRegistry {

    public static final ResourceKey<Registry<Action<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "editor_action_type"));

    public static final Registry<Action<?>> REGISTRY = new MappedRegistry<>(REGISTRY_KEY, Lifecycle.stable());

    private static final Map<Class<?>, Action<?>> BY_CLASS = new ConcurrentHashMap<>();

    private static final StreamCodec<ByteBuf, EditorAction<?>> STREAM_CODEC = StreamCodec.of(
        (buffer, action) -> {
            Action<?> type = getByClass(action.getClass());
            Identifier.STREAM_CODEC.encode(buffer, type.id());
            type.encode(buffer, action);
        },
        buffer -> {
            Identifier id = Identifier.STREAM_CODEC.decode(buffer);
            Action<?> type = get(id);
            if (type == null)
                throw new IllegalArgumentException("Unknown action type: " + id);
            return type.decode(buffer);
        }
    );

    public static <A extends EditorAction<?>> Action<A> register(String path, StreamCodec<ByteBuf, A> codec, Class<A> actionClass) {
        return register(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, path), codec, actionClass);
    }

    public static <A extends EditorAction<?>> Action<A> register(Identifier id, StreamCodec<ByteBuf, A> codec, Class<A> actionClass) {
        Action<A> type = new Action<>(id, actionClass, codec);
        Registry.register(REGISTRY, id, type);
        BY_CLASS.put(actionClass, type);
        return type;
    }

    public static Action<?> get(Identifier id) {
        if (id == null)
            return null;
        freeze();
        return REGISTRY.getValue(id);
    }

    public static Action<?> getByClass(Class<?> actionClass) {
        Action<?> type = BY_CLASS.get(actionClass);
        if (type == null)
            throw new IllegalArgumentException("Unknown action class: " + actionClass.getName());
        return type;
    }

    public static StreamCodec<ByteBuf, EditorAction<?>> streamCodec() {
        return STREAM_CODEC;
    }

    public static void freeze() {
        REGISTRY.freeze();
    }

    private EditorActionRegistry() {
    }
}
