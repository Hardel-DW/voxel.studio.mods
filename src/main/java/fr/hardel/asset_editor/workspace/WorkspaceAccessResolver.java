package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.workspace.io.DataPackManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class WorkspaceAccessResolver {

    public Resolution resolveEditable(ServerPlayer player, MinecraftServer server, String packId, Identifier registryId) {
        PermissionManager permissionManager = PermissionManager.get();
        if (permissionManager == null)
            return new Resolution.Failure("error:server_unavailable");

        if (!permissionManager.getEffectivePermissions(player).canEdit())
            return new Resolution.Failure("error:permission_denied");

        WorkspaceRepository repository = WorkspaceRepository.get();
        DataPackManager packManager = DataPackManager.get();
        if (repository == null || packManager == null)
            return new Resolution.Failure("error:server_unavailable");

        var definition = WorkspaceDefinition.get(registryId);
        if (definition == null)
            return new Resolution.Failure("error:invalid_registry");

        var packRoot = packManager.resolveWritablePack(packId);
        if (packRoot.isEmpty())
            return new Resolution.Failure("error:invalid_pack");

        return new Resolution.Success(player, server, packId, packRoot.get(), repository, definition);
    }

    public sealed interface Resolution permits Resolution.Success, Resolution.Failure {

        record Success(ServerPlayer player, MinecraftServer server, String packId,
            Path packRoot, WorkspaceRepository repository, WorkspaceDefinition<?> definition) implements Resolution {

            public HolderLookup.Provider registries() {
                return server.registryAccess();
            }
        }

        record Failure(String errorCode) implements Resolution {}
    }
}
