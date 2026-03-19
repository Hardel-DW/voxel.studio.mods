package fr.hardel.asset_editor.workspace.service;

import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.store.ServerPackManager;
import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBindings;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WorkspaceAccessResolver {

    public Resolution resolveEditable(ServerPlayer player, MinecraftServer server, String packId, Identifier registryId) {
        PermissionManager permissionManager = PermissionManager.get();
        if (permissionManager == null)
            return new Resolution.Failure("error:server_unavailable");

        if (!permissionManager.getEffectivePermissions(player).canEdit())
            return new Resolution.Failure("error:permission_denied");

        WorkspaceRepository repository = WorkspaceRepository.get();
        ServerPackManager packManager = ServerPackManager.get();
        if (repository == null || packManager == null)
            return new Resolution.Failure("error:server_unavailable");

        var binding = RegistryWorkspaceBindings.get(registryId);
        if (binding == null)
            return new Resolution.Failure("error:invalid_registry");

        var packRoot = packManager.resolveWritablePack(packId);
        if (packRoot.isEmpty())
            return new Resolution.Failure("error:invalid_pack");

        return new Resolution.Success(new ResolvedWorkspaceAccess(player, server, packId, packRoot.get(), repository, binding));
    }

    public sealed interface Resolution permits Resolution.Success, Resolution.Failure {
        record Success(ResolvedWorkspaceAccess access) implements Resolution {}

        record Failure(String errorCode) implements Resolution {}
    }
}
