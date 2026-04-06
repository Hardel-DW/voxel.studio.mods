package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.workspace.io.DataPackManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class ServerPackService {

    public Optional<List<DataPackManager.PackEntry>> listPacks() {
        DataPackManager packManager = DataPackManager.get();
        if (packManager == null)
            return Optional.empty();

        return Optional.of(packManager.listPacks());
    }

    public Optional<List<DataPackManager.PackEntry>> createPack(ServerPlayer player, String name, String namespace) {
        PermissionManager permissionManager = PermissionManager.get();
        if (permissionManager == null)
            return Optional.empty();

        if (!permissionManager.getEffectivePermissions(player).canEdit())
            return Optional.empty();

        DataPackManager packManager = DataPackManager.get();
        if (packManager == null)
            return Optional.empty();

        packManager.createPack(name, namespace);
        return Optional.of(packManager.listPacks());
    }
}
