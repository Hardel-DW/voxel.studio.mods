package fr.hardel.asset_editor.client.javafx.lib.action;

import fr.hardel.asset_editor.client.state.ClientPackInfo;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.client.state.ClientWorkspaceState;
import fr.hardel.asset_editor.client.state.PendingClientAction;
import fr.hardel.asset_editor.network.pack.PackWorkspaceRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBindings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EditorActionGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorActionGateway.class);

    private final ClientSessionState sessionState;
    private final ClientWorkspaceState workspaceState;

    public EditorActionGateway(ClientSessionState sessionState, ClientWorkspaceState workspaceState) {
        this.sessionState = sessionState;
        this.workspaceState = workspaceState;
    }

    public <T> EditorActionResult dispatch(ResourceKey<Registry<T>> registry, Identifier target, EditorAction action) {
        EditorActionResult check = validate(target);
        if (check != null)
            return check;

        ClientPackInfo pack = workspaceState.packSelectionState().selectedPack();
        if (pack == null)
            return EditorActionResult.packRequired();

        ElementEntry<T> entry = workspaceState.elementStore().get(registry, target);
        if (entry == null)
            return EditorActionResult.error("error:element_not_found");

        UUID actionId = UUID.randomUUID();
        workspaceState.trackPendingAction(actionId, new PendingClientAction<>(actionId, pack.packId(), registry, target, entry));

        projectOptimistic(registry, target, entry, action);

        ClientPlayNetworking.send(new WorkspaceMutationRequestPayload(
            actionId,
            pack.packId(),
            registry.identifier(),
            target,
            action));
        return EditorActionResult.applied();
    }

    private <T> void projectOptimistic(ResourceKey<Registry<T>> registry, Identifier target,
        ElementEntry<T> entry, EditorAction action) {
        RegistryWorkspaceBinding<T> binding = RegistryWorkspaceBindings.get(registry.identifier());
        if (binding == null || binding.interpreter() == null)
            return;

        HolderLookup.Provider registries = clientRegistries();
        if (registries == null)
            return;

        try {
            ElementEntry<T> projected = binding.interpreter().apply(entry, action, registries);
            if (projected != null && !Objects.equals(projected, entry))
                workspaceState.elementStore().put(registry, target, projected);
        } catch (Exception e) {
            LOGGER.warn("Optimistic projection failed for {}: {}", target, e.getMessage());
        }
    }

    public void requestPackWorkspace(ResourceKey<?> registry) {
        ClientPackInfo pack = workspaceState.packSelectionState().selectedPack();
        if (pack == null || pack.packId().isBlank())
            return;
        ClientPlayNetworking.send(new PackWorkspaceRequestPayload(pack.packId(), registry.identifier()));
    }

    public void handleWorkspaceSync(WorkspaceSyncPayload payload) {
        if (!payload.mutationResponse()) {
            if (payload.snapshot() != null)
                applySnapshotIfCurrentPack(payload.packId(), payload.snapshot());
            return;
        }

        PendingClientAction<?> pending = workspaceState.removePendingAction(payload.actionId());
        if (pending == null)
            return;
        if (payload.accepted()) {
            if (payload.snapshot() != null)
                applySnapshotIfCurrentPack(payload.packId(), payload.snapshot());
            return;
        }
        restorePending(pending);
        workspaceState.issueState().pushError(payload.errorCode());
    }

    public void handlePackWorkspaceSync(String packId, Identifier registryId, List<WorkspaceElementSnapshot> snapshots) {
        ClientPackInfo selectedPack = workspaceState.packSelectionState().selectedPack();
        if (selectedPack == null || !selectedPack.packId().equals(packId))
            return;

        var binding = RegistryWorkspaceBindings.get(registryId);
        if (binding == null)
            return;

        HolderLookup.Provider registries = clientRegistries();
        if (registries == null)
            return;

        replaceAll(binding, snapshots, registries);
    }

    private void restorePending(PendingClientAction<?> pending) {
        ClientPackInfo selectedPack = workspaceState.packSelectionState().selectedPack();
        if (selectedPack == null || !selectedPack.packId().equals(pending.packId()))
            return;
        restorePendingTyped(pending);
    }

    private <T> void restorePendingTyped(PendingClientAction<T> pending) {
        workspaceState.elementStore().put(
            pending.registry(),
            pending.target(),
            pending.previousSnapshot());
    }

    private void applySnapshotIfCurrentPack(String packId, WorkspaceElementSnapshot snapshot) {
        ClientPackInfo selectedPack = workspaceState.packSelectionState().selectedPack();
        if (selectedPack == null || !selectedPack.packId().equals(packId))
            return;

        var binding = RegistryWorkspaceBindings.get(snapshot.registryId());
        if (binding == null)
            return;

        HolderLookup.Provider registries = clientRegistries();
        if (registries == null)
            return;

        applySnapshot(binding, snapshot, registries);
    }

    private <T> void replaceAll(RegistryWorkspaceBinding<T> binding, List<WorkspaceElementSnapshot> snapshots,
        HolderLookup.Provider registries) {
        List<ElementEntry<T>> entries = snapshots.stream()
            .map(snapshot -> binding.fromSnapshot(snapshot, registries))
            .toList();
        workspaceState.elementStore().replaceAll(binding.registryKey(), entries);
    }

    private <T> void applySnapshot(RegistryWorkspaceBinding<T> binding, WorkspaceElementSnapshot snapshot,
        HolderLookup.Provider registries) {
        ElementEntry<T> entry = binding.fromSnapshot(snapshot, registries);
        workspaceState.elementStore().put(binding.registryKey(), snapshot.targetId(), entry);
    }

    private HolderLookup.Provider clientRegistries() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null ? connection.registryAccess() : null;
    }

    private EditorActionResult validate(Identifier target) {
        if (target == null)
            return EditorActionResult.error("error:element_not_found");
        var pack = workspaceState.packSelectionState().selectedPack();
        if (pack == null)
            return EditorActionResult.packRequired();
        if (!pack.writable())
            return EditorActionResult.rejected("error:pack_readonly");
        if (!sessionState.permissions().canEdit())
            return EditorActionResult.rejected("error:permission_denied");
        return null;
    }
}
