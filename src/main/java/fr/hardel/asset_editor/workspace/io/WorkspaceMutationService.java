package fr.hardel.asset_editor.workspace.io;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.RegistryMutationContexts;
import fr.hardel.asset_editor.workspace.TagResourceService;
import fr.hardel.asset_editor.workspace.access.ResolvedWorkspaceAccess;
import fr.hardel.asset_editor.workspace.access.WorkspaceAccessResolver;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
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
        var resolution = accessResolver.resolveEditable(player, server, payload.packId(), payload.registryId());
        if (resolution instanceof WorkspaceAccessResolver.Resolution.Failure(String errorCode))
            return new MutationResult.Failure(errorCode);

        var access = ((WorkspaceAccessResolver.Resolution.Success) resolution).access();
        return applyMutation(access.definition(), access, payload);
    }

    private <T> MutationResult applyMutation(WorkspaceDefinition<T> definition, ResolvedWorkspaceAccess access, WorkspaceMutationRequestPayload payload) {
        ElementEntry<T> entry = access.repository().get(access.packId(), definition, access.packRoot(), access.registries(), payload.targetId());
        if (entry == null)
            return new MutationResult.Failure("error:element_not_found");

        RegistryMutationContext context = RegistryMutationContexts.server(access.packRoot(), access.registries(), tagResources, tagReferences);
        ElementEntry<T> updated;
        try {
            definition.beforeApply(payload.action(), context);
            updated = definition.apply(entry, payload.action(), context);
        } catch (Exception e) {
            LOGGER.warn("Action rejected for {}: {}", payload.targetId(), e.getMessage());
            return new MutationResult.Failure("error:invalid_action");
        }

        WorkspaceElementSnapshot snapshot;
        try {
            snapshot = definition.toSnapshot(updated, access.registries());
        } catch (Exception e) {
            LOGGER.warn("Mutation rejected for {}: {}", payload.targetId(), e.getMessage());
            return new MutationResult.Failure("error:invalid_action");
        }

        access.repository().put(access.packId(), definition, access.packRoot(), access.registries(), payload.targetId(), updated);
        access.repository().flushDirty(access.packRoot(), access.packId(), definition, access.registries());
        return new MutationResult.Success(access.packId(), snapshot);
    }

    public sealed interface MutationResult permits MutationResult.Success, MutationResult.Failure {
        record Success(String packId, WorkspaceElementSnapshot snapshot) implements MutationResult {}
        record Failure(String errorCode) implements MutationResult {}
    }
}
