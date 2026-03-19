package fr.hardel.asset_editor.workspace.service;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkspaceMutationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceMutationService.class);

    private final WorkspaceAccessResolver accessResolver;

    public WorkspaceMutationService(WorkspaceAccessResolver accessResolver) {
        this.accessResolver = accessResolver;
    }

    public MutationResult mutate(ServerPlayer player, MinecraftServer server, WorkspaceMutationRequestPayload payload) {
        WorkspaceAccessResolver.Resolution resolution = accessResolver.resolveEditable(player, server, payload.packId(), payload.registryId());
        if (resolution instanceof WorkspaceAccessResolver.Resolution.Failure failure)
            return new MutationResult.Failure(failure.errorCode());

        return mutateTyped(((WorkspaceAccessResolver.Resolution.Success) resolution).access(), payload);
    }

    private <T> MutationResult mutateTyped(ResolvedWorkspaceAccess access, WorkspaceMutationRequestPayload payload) {
        RegistryWorkspaceBinding<T> binding = access.bindingTyped();
        ElementEntry<T> entry = access.repository().get(access.packId(), binding, access.packRoot(), access.registries(), payload.targetId());
        if (entry == null)
            return new MutationResult.Failure("error:element_not_found");

        ElementEntry<T> updated;
        try {
            updated = binding.interpreter().apply(entry, payload.action(), access.registries());
        } catch (Exception e) {
            LOGGER.warn("Action rejected for {}: {}", payload.targetId(), e.getMessage());
            return new MutationResult.Failure("error:invalid_action");
        }

        access.repository().put(access.packId(), binding, access.packRoot(), access.registries(), payload.targetId(), updated);
        access.repository().flushDirty(access.packRoot(), access.packId(), binding, access.registries());
        return new MutationResult.Success(access.packId(), binding.toSnapshot(updated, access.registries()));
    }

    public sealed interface MutationResult permits MutationResult.Success, MutationResult.Failure {
        record Success(String packId, WorkspaceElementSnapshot snapshot) implements MutationResult {}

        record Failure(String errorCode) implements MutationResult {}
    }
}
