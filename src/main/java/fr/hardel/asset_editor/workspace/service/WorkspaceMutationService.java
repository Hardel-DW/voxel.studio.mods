package fr.hardel.asset_editor.workspace.service;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.workspace.TagResourceService;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContexts;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationHandler;
import fr.hardel.asset_editor.workspace.registry.MutationHandlerRegistry;
import fr.hardel.asset_editor.tag.TagReferenceResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkspaceMutationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceMutationService.class);

    private final WorkspaceAccessResolver accessResolver;
    private final TagResourceService tagResources = new TagResourceService();
    private final TagReferenceResolver tagReferences = new TagReferenceResolver();

    public WorkspaceMutationService(WorkspaceAccessResolver accessResolver) {
        this.accessResolver = accessResolver;
    }

    public MutationResult mutate(ServerPlayer player, MinecraftServer server, WorkspaceMutationRequestPayload payload) {
        WorkspaceAccessResolver.Resolution resolution = accessResolver.resolveEditable(player, server, payload.packId(), payload.registryId());
        if (resolution instanceof WorkspaceAccessResolver.Resolution.Failure failure)
            return new MutationResult.Failure(failure.errorCode());

        return mutateTyped(((WorkspaceAccessResolver.Resolution.Success) resolution).access(), payload);
    }

    private MutationResult mutateTyped(ResolvedWorkspaceAccess access, WorkspaceMutationRequestPayload payload) {
        return mutateTyped(access.binding(), access, payload);
    }

    private <T> MutationResult mutateTyped(
        RegistryWorkspaceBinding<T> binding,
        ResolvedWorkspaceAccess access,
        WorkspaceMutationRequestPayload payload
    ) {
        ElementEntry<T> entry = access.repository().get(access.packId(), binding, access.packRoot(), access.registries(), payload.targetId());
        if (entry == null)
            return new MutationResult.Failure("error:element_not_found");

        RegistryMutationContext context = RegistryMutationContexts.server(access.packRoot(), access.registries(), tagResources, tagReferences);
        RegistryMutationHandler<T> mutationHandler = MutationHandlerRegistry.get(binding.registryKey());
        if (mutationHandler == null)
            mutationHandler = RegistryMutationHandler.unsupported();
        ElementEntry<T> updated;
        try {
            mutationHandler.beforeApply(payload.action(), context);
            updated = mutationHandler.apply(entry, payload.action(), context);
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
