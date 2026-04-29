package fr.hardel.asset_editor.network.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class UnsyncedRegistryCatalog {

    private static final List<ResourceKey<? extends Registry<?>>> EXPOSED_REGISTRIES = List.of(
        Registries.TEMPLATE_POOL
    );

    public static List<RegistryIdSnapshot> build(MinecraftServer server) {
        return EXPOSED_REGISTRIES.stream()
            .map(key -> snapshot(server, key))
            .flatMap(Optional::stream)
            .toList();
    }

    private static Optional<RegistryIdSnapshot> snapshot(MinecraftServer server, ResourceKey<? extends Registry<?>> key) {
        return server.registryAccess().lookup(key).map(registry -> new RegistryIdSnapshot(
            key.identifier(),
            registry.listElementIds()
                .map(ResourceKey::identifier)
                .sorted(Comparator.comparing(Identifier::toString))
                .toList()));
    }

    private UnsyncedRegistryCatalog() {}
}
