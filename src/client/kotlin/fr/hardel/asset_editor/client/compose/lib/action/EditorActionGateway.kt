package fr.hardel.asset_editor.client.compose.lib.action

import fr.hardel.asset_editor.client.WorkspaceSyncGateway
import fr.hardel.asset_editor.client.debug.ClientDebugTelemetry
import fr.hardel.asset_editor.client.memory.session.SessionMemory
import fr.hardel.asset_editor.client.memory.persistent.IssueMemory
import fr.hardel.asset_editor.client.memory.session.ui.ClientPackInfo
import fr.hardel.asset_editor.client.memory.session.server.ClientWorkspaceRegistries
import fr.hardel.asset_editor.client.memory.session.server.ClientWorkspaceRegistry
import fr.hardel.asset_editor.client.memory.session.ui.PackSelectionMemory
import fr.hardel.asset_editor.client.memory.session.server.RegistryMemory
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.pack.PackWorkspaceRequestPayload
import fr.hardel.asset_editor.network.workspace.ElementSeedRequestPayload
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload
import fr.hardel.asset_editor.workspace.flush.ElementEntry
import fr.hardel.asset_editor.workspace.action.EditorAction

import fr.hardel.asset_editor.workspace.io.RegistryMutationContexts
import java.util.Objects
import java.util.UUID
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.core.HolderLookup
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

class EditorActionGateway(
    private val sessionMemory: SessionMemory,
    private val packSelection: PackSelectionMemory,
    private val registryMemory: RegistryMemory,
    private val issues: IssueMemory
) : WorkspaceSyncGateway {

    private val pendingActions = LinkedHashMap<UUID, PendingClientAction<*>>()

    fun pendingActionCount(): Int = pendingActions.size

    fun clearPendingActions() {
        pendingActions.clear()
    }

    private fun trackPendingAction(actionId: UUID, action: PendingClientAction<*>) {
        pendingActions[actionId] = action
    }

    private fun removePendingAction(actionId: UUID): PendingClientAction<*>? {
        return pendingActions.remove(actionId)
    }

    fun <T : Any> dispatch(
        workspace: ClientWorkspaceRegistry<T>,
        target: Identifier?,
        action: EditorAction<*>
    ): EditorActionResult {
        val check = validate(target)
        if (check != null) {
            return check
        }

        val pack = packSelection.selectedPack()
            ?: return EditorActionResult.packRequired()
        val resolvedTarget = target ?: return EditorActionResult.error("error:workspace_sync_pending")
        val entry = registryMemory.get(workspace, resolvedTarget)
            ?: return EditorActionResult.error("error:workspace_sync_pending")

        val actionId = UUID.randomUUID()
        trackPendingAction(
            actionId,
            PendingClientAction(actionId, pack.packId(), workspace, resolvedTarget, entry)
        )

        projectOptimistic(workspace, resolvedTarget, entry, action)
        val payload = WorkspaceMutationRequestPayload(
            actionId,
            pack.packId(),
            workspace.registryId(),
            resolvedTarget,
            action
        )
        ClientDebugTelemetry.actionDispatched(pack.packId(), workspace.registryId(), resolvedTarget, action, actionId)
        ClientPayloadSender.send(payload)
        return EditorActionResult.applied()
    }

    fun requestPackWorkspace(workspace: ClientWorkspaceRegistry<*>) {
        val pack = packSelection.selectedPack()
        if (pack == null || pack.packId().isBlank()) return
        ClientPayloadSender.send(PackWorkspaceRequestPayload(pack.packId(), workspace.registryId()))
    }

    fun requestElementSeed(workspace: ClientWorkspaceRegistry<*>, elementId: Identifier) {
        val pack = packSelection.selectedPack()
        if (pack == null || pack.packId().isBlank()) return
        ClientPayloadSender.send(ElementSeedRequestPayload(pack.packId(), workspace.registryId(), elementId))
    }

    override fun handleWorkspaceSync(payload: WorkspaceSyncPayload) {
        if (!payload.mutationResponse()) {
            payload.snapshot()?.let { applySnapshotIfCurrentPack(payload.packId(), it) }
            return
        }

        val pending = removePendingAction(payload.actionId()) ?: return
        if (payload.accepted()) {
            payload.snapshot()?.let { applySnapshotIfCurrentPack(payload.packId(), it) }
            return
        }

        restorePending(pending)
        ClientDebugTelemetry.actionRejected(payload.actionId(), payload.errorCode() ?: "unknown")
        issues.pushError(payload.errorCode())
    }

    override fun handlePackWorkspaceSync(
        packId: String,
        registryId: Identifier,
        snapshots: List<WorkspaceElementSnapshot>
    ) {
        val selectedPack = packSelection.selectedPack()
        if (selectedPack == null || selectedPack.packId() != packId) {
            return
        }

        val workspace = ClientWorkspaceRegistries.get(registryId) ?: return
        val registries = clientRegistries() ?: return
        replaceAll(workspace, snapshots, registries)
    }

    private fun <T : Any> projectOptimistic(
        workspace: ClientWorkspaceRegistry<T>,
        target: Identifier,
        entry: ElementEntry<T>,
        action: EditorAction<*>
    ) {
        val registries = clientRegistries() ?: return

        try {
            val context = RegistryMutationContexts.client(registries)
            val projected = workspace.definition().apply(entry, action, context)
            if (!Objects.equals(projected, entry)) {
                registryMemory.put(workspace, target, projected)
            }
        } catch (exception: Exception) {
            LOGGER.warn("Optimistic projection failed for {}: {}", target, exception.message)
            ClientDebugTelemetry.optimisticFailed(workspace.registryId(), target, action, exception.message ?: "unknown")
        }
    }

    private fun restorePending(pending: PendingClientAction<*>) {
        val selectedPack = packSelection.selectedPack()
        if (selectedPack == null || selectedPack.packId() != pending.packId) {
            return
        }

        pending.restoreInto(registryMemory)
    }

    private fun applySnapshotIfCurrentPack(packId: String, snapshot: WorkspaceElementSnapshot) {
        val selectedPack = packSelection.selectedPack()
        if (selectedPack == null || selectedPack.packId() != packId) {
            return
        }

        val workspace = ClientWorkspaceRegistries.get(snapshot.registryId()) ?: return
        val registries = clientRegistries() ?: return
        applySnapshot(workspace, snapshot, registries)
    }

    private fun replaceAll(
        workspace: ClientWorkspaceRegistry<*>,
        snapshots: List<WorkspaceElementSnapshot>,
        registries: HolderLookup.Provider
    ) {
        replaceAllTyped(workspace, snapshots, registries)
    }

    private fun <T : Any> replaceAllTyped(
        workspace: ClientWorkspaceRegistry<T>,
        snapshots: List<WorkspaceElementSnapshot>,
        registries: HolderLookup.Provider
    ) {
        val entries = snapshots.map { workspace.definition().fromSnapshot(it, registries) }
        registryMemory.replaceAll(workspace, entries)
    }

    private fun applySnapshot(
        workspace: ClientWorkspaceRegistry<*>,
        snapshot: WorkspaceElementSnapshot,
        registries: HolderLookup.Provider
    ) {
        applySnapshotTyped(workspace, snapshot, registries)
    }

    private fun <T : Any> applySnapshotTyped(
        workspace: ClientWorkspaceRegistry<T>,
        snapshot: WorkspaceElementSnapshot,
        registries: HolderLookup.Provider
    ) {
        val entry = workspace.definition().fromSnapshot(snapshot, registries)
        registryMemory.put(workspace, snapshot.targetId(), entry)
    }

    private fun clientRegistries(): HolderLookup.Provider? {
        val connection: ClientPacketListener? = Minecraft.getInstance().connection
        return connection?.registryAccess()
    }

    private fun validate(target: Identifier?): EditorActionResult? {
        if (target == null) {
            return EditorActionResult.error("error:workspace_sync_pending")
        }

        val pack: ClientPackInfo = packSelection.selectedPack()
            ?: return EditorActionResult.packRequired()
        if (!pack.writable()) {
            return EditorActionResult.rejected("error:pack_readonly")
        }
        if (!sessionMemory.permissions().canEdit()) {
            return EditorActionResult.rejected("error:permission_denied")
        }

        return null
    }

    data class PendingClientAction<T : Any>(
        val actionId: UUID,
        val packId: String,
        val workspace: ClientWorkspaceRegistry<T>,
        val target: Identifier,
        val previousSnapshot: ElementEntry<T>
    ) {
        fun restoreInto(memory: RegistryMemory) {
            memory.put(workspace, target, previousSnapshot)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EditorActionGateway::class.java)
    }
}
