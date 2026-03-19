package fr.hardel.asset_editor.workspace.service;

import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class WorkspaceQueryService {

    private final WorkspaceAccessResolver accessResolver;

    public WorkspaceQueryService(WorkspaceAccessResolver accessResolver) {
        this.accessResolver = accessResolver;
    }

    public Optional<PackWorkspaceSyncPayload> loadPackWorkspace(ServerPlayer player, MinecraftServer server,
        String packId, Identifier registryId) {
        WorkspaceAccessResolver.Resolution resolution = accessResolver.resolveEditable(player, server, packId, registryId);
        if (!(resolution instanceof WorkspaceAccessResolver.Resolution.Success success))
            return Optional.empty();

        return Optional.of(loadPackWorkspaceTyped(success.access()));
    }

    private <T> PackWorkspaceSyncPayload loadPackWorkspaceTyped(ResolvedWorkspaceAccess access) {
        RegistryWorkspaceBinding<T> binding = access.bindingTyped();
        List<ElementEntry<T>> entries = access.repository().snapshotWorkspace(access.packId(), binding, access.packRoot(), access.registries());
        List<WorkspaceElementSnapshot> snapshots = entries.stream()
            .map(entry -> binding.toSnapshot(entry, access.registries()))
            .toList();

        return new PackWorkspaceSyncPayload(access.packId(), binding.registryId(), snapshots);
    }
}
