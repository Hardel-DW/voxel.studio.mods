package fr.hardel.asset_editor.client.compose.lib.action

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload
import net.minecraft.resources.Identifier

interface WorkspaceSyncGateway {
    fun handleWorkspaceSync(payload: WorkspaceSyncPayload)

    fun handlePackWorkspaceSync(
        packId: String,
        registryId: Identifier,
        snapshots: List<WorkspaceElementSnapshot>
    )
}
