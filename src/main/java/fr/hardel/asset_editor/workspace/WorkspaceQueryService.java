package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class WorkspaceQueryService {

    private final WorkspaceAccessResolver accessResolver;

    public WorkspaceQueryService(WorkspaceAccessResolver accessResolver) {
        this.accessResolver = accessResolver;
    }

    public Optional<PackWorkspaceSyncPayload> loadPackWorkspace(ServerPlayer player, MinecraftServer server,
        String packId, Identifier registryId) {
        var resolution = accessResolver.resolveEditable(player, server, packId, registryId);
        if (!(resolution instanceof WorkspaceAccessResolver.Resolution.Success access))
            return Optional.empty();

        return Optional.of(buildPayload(access.definition(), access));
    }

    private <T> PackWorkspaceSyncPayload buildPayload(WorkspaceDefinition<T> definition, WorkspaceAccessResolver.Resolution.Success access) {
        List<ElementEntry<T>> entries = access.repository().snapshotWorkspace(access.packId(), definition, access.packRoot(), access.registries());
        List<WorkspaceElementSnapshot> snapshots = entries.stream()
            .map(entry -> definition.toSnapshot(entry, access.registries()))
            .toList();
        Set<Identifier> modifiedIds = access.repository().modifiedIds(access.packId(), definition, access.packRoot(), access.registries());

        return new PackWorkspaceSyncPayload(access.packId(), definition.registryId(), snapshots, modifiedIds);
    }
}
