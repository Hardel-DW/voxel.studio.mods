package fr.hardel.asset_editor.workspace.service;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.permission.PermissionManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WorkspaceBroadcastService {

    public void broadcastMutation(MinecraftServer server, ServerPlayer sender, String packId, WorkspaceElementSnapshot snapshot) {
        PermissionManager permissionManager = PermissionManager.get();
        if (permissionManager == null)
            return;

        WorkspaceSyncPayload payload = WorkspaceSyncPayload.remoteSync(packId, snapshot);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == sender)
                continue;

            if (!permissionManager.getEffectivePermissions(other).canEdit())
                continue;

            ServerPlayNetworking.send(other, payload);
        }
    }
}
