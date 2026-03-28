package fr.hardel.asset_editor.workspace.service;

import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public record ResolvedWorkspaceAccess(ServerPlayer player, MinecraftServer server, String packId, Path packRoot, WorkspaceRepository repository, RegistryWorkspaceBinding<?> binding) {

    public HolderLookup.Provider registries() {
        return server.registryAccess();
    }
}
