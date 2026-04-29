package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContexts;
import fr.hardel.asset_editor.workspace.io.TagResourceService;
import fr.hardel.asset_editor.tag.TagReferenceResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WorkspaceMutationService {

    private final WorkspaceAccessResolver accessResolver;
    private final TagResourceService tagResources = new TagResourceService();
    private final TagReferenceResolver tagReferences = new TagReferenceResolver();

    public WorkspaceMutationService(WorkspaceAccessResolver accessResolver) {
        this.accessResolver = accessResolver;
    }

    public MutationResult mutate(ServerPlayer player, MinecraftServer server, WorkspaceMutationRequestPayload payload) {
        var resolution = accessResolver.resolveEditable(player, server, payload.packId(), payload.registryId());
        if (resolution instanceof WorkspaceAccessResolver.Resolution.Failure(String errorCode))
            return new MutationResult.Failure(errorCode, "");

        var access = (WorkspaceAccessResolver.Resolution.Success) resolution;
        return applyMutation(access.definition(), access, payload);
    }

    private <T> MutationResult applyMutation(WorkspaceDefinition<T> definition, WorkspaceAccessResolver.Resolution.Success access, WorkspaceMutationRequestPayload payload) {
        ElementEntry<T> entry = access.repository().get(access.packId(), definition, access.packRoot(), access.registries(), payload.targetId());
        if (entry == null)
            return new MutationResult.Failure("error:element_not_found", "");

        RegistryMutationContext context = RegistryMutationContexts.server(access.packRoot(), access.registries(), tagResources, tagReferences);
        ElementEntry<T> updated;
        try {
            updated = definition.apply(entry, payload.action(), context);
        } catch (Exception e) {
            return new MutationResult.Failure("error:invalid_action", detailFor(payload, e));
        }

        WorkspaceElementSnapshot snapshot;
        try {
            snapshot = definition.toSnapshot(updated, access.registries());
        } catch (Exception e) {
            return new MutationResult.Failure("error:invalid_action", detailFor(payload, e));
        }

        access.repository().put(access.packId(), definition, access.packRoot(), access.registries(), payload.targetId(), updated);
        access.repository().flushDirty(access.packRoot(), access.packId(), definition, access.registries());
        boolean modifiedVsReference = access.repository().isModifiedVsReference(
            access.packId(), definition, access.packRoot(), access.registries(), payload.targetId());
        return new MutationResult.Success(access.packId(), snapshot, modifiedVsReference);
    }

    private static String detailFor(WorkspaceMutationRequestPayload payload, Exception e) {
        String message = e.getMessage();
        return payload.targetId() + ": " + (message == null || message.isBlank() ? e.getClass().getSimpleName() : message);
    }

    public sealed interface MutationResult permits MutationResult.Success, MutationResult.Failure {
        record Success(String packId, WorkspaceElementSnapshot snapshot, boolean modifiedVsReference) implements MutationResult {}

        record Failure(String errorCode, String errorDetail) implements MutationResult {}
    }
}
