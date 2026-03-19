package fr.hardel.asset_editor.store;

import fr.hardel.asset_editor.permission.PermissionManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class ServerPackService {

    public Optional<List<ServerPackManager.PackEntry>> listPacks() {
        ServerPackManager packManager = ServerPackManager.get();
        if (packManager == null)
            return Optional.empty();

        return Optional.of(packManager.listPacks());
    }

    public Optional<List<ServerPackManager.PackEntry>> createPack(ServerPlayer player, String name, String namespace) {
        PermissionManager permissionManager = PermissionManager.get();
        if (permissionManager == null)
            return Optional.empty();

        if (!permissionManager.getEffectivePermissions(player).canEdit())
            return Optional.empty();

        ServerPackManager packManager = ServerPackManager.get();
        if (packManager == null)
            return Optional.empty();

        packManager.createPack(name, namespace);
        return Optional.of(packManager.listPacks());
    }
}
