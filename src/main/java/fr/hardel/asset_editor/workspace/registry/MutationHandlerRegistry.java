package fr.hardel.asset_editor.workspace.registry;

import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.AssetEditor;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class MutationHandlerRegistry {

    public static final ResourceKey<Registry<RegistryMutationHandler<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "registry_mutation_handler"));

    public static final Registry<RegistryMutationHandler<?>> REGISTRY = new MappedRegistry<>(REGISTRY_KEY, Lifecycle.stable());

    public static <T> RegistryMutationHandler<T> register(
        ResourceKey<Registry<T>> registryKey,
        RegistryMutationHandler<T> handler
    ) {
        if (registryKey == null)
            throw new IllegalArgumentException("Registry key cannot be null");
        if (handler == null)
            throw new IllegalArgumentException("Mutation handler cannot be null");

        Registry.register(REGISTRY, registryKey.identifier(), handler);
        return handler;
    }

    public static <T> RegistryMutationHandler<T> get(ResourceKey<Registry<T>> registryKey) {
        if (registryKey == null)
            return null;

        freeze();
        return cast(REGISTRY.getValue(registryKey.identifier()));
    }

    public static void freeze() {
        REGISTRY.freeze();
    }

    @SuppressWarnings("unchecked")
    private static <T> RegistryMutationHandler<T> cast(RegistryMutationHandler<?> handler) {
        return (RegistryMutationHandler<T>) handler;
    }

    private MutationHandlerRegistry() {
    }
}
