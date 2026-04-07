package fr.hardel.asset_editor.workspace.action;

import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorActionRegistry {

    public static final ResourceKey<Registry<Action<?, ?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "editor_action_type"));

    public static final Registry<Action<?, ?>> REGISTRY = new MappedRegistry<>(REGISTRY_KEY, Lifecycle.stable());

    private static final Map<Class<?>, Action<?, ?>> BY_CLASS = new ConcurrentHashMap<>();

    private static final StreamCodec<ByteBuf, EditorAction<?>> STREAM_CODEC = StreamCodec.of(
        (buffer, action) -> {
            Action<?, ?> type = getByClass(action.getClass());
            Identifier.STREAM_CODEC.encode(buffer, type.id());
            type.encode(buffer, action);
        },
        buffer -> {
            Identifier id = Identifier.STREAM_CODEC.decode(buffer);
            Action<?, ?> type = get(id);
            if (type == null)
                throw new IllegalArgumentException("Unknown action type: " + id);
            return type.decode(buffer);
        });

    public static <T, A extends EditorAction<T>> Action<T, A> register(ResourceKey<Registry<T>> registryKey, String path, StreamCodec<ByteBuf, A> codec, Class<A> actionClass) {
        return register(registryKey, Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, path), codec, actionClass);
    }

    public static <T, A extends EditorAction<T>> Action<T, A> register(ResourceKey<Registry<T>> registryKey, Identifier id, StreamCodec<ByteBuf, A> codec, Class<A> actionClass) {
        WorkspaceDefinition<T> definition = WorkspaceDefinition.get(registryKey);
        if (definition == null)
            throw new IllegalStateException("Workspace definition must be registered before actions for " + registryKey.identifier());

        if (BY_CLASS.containsKey(actionClass))
            throw new IllegalStateException("Duplicate action class: " + actionClass.getName());

        Action<T, A> type = new Action<>(id, actionClass, codec);
        Registry.register(REGISTRY, id, type);
        definition.registerAction(type);
        BY_CLASS.put(actionClass, type);
        return type;
    }

    public static Action<?, ?> get(Identifier id) {
        if (id == null)
            return null;
        freeze();
        return REGISTRY.getValue(id);
    }

    public static Action<?, ?> getByClass(Class<?> actionClass) {
        Action<?, ?> type = BY_CLASS.get(actionClass);
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

    private EditorActionRegistry() {}
}
