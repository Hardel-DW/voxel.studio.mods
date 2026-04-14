package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;

public interface WorkspaceSyncGateway {
    void handleWorkspaceSync(WorkspaceSyncPayload payload);

    void handlePackWorkspaceSync(String packId, Identifier registryId, List<WorkspaceElementSnapshot> snapshots, Set<Identifier> modifiedIds);
}
