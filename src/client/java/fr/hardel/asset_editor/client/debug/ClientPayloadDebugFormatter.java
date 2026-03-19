package fr.hardel.asset_editor.client.debug;

import fr.hardel.asset_editor.network.pack.PackCreatePayload;
import fr.hardel.asset_editor.network.pack.PackListRequestPayload;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceRequestPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.session.PermissionSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ClientPayloadDebugFormatter {

    public record Details(Identifier id, String title, String description, Map<String, String> data) {}

    private record Registration<T>(Class<T> type, BiConsumer<T, Map<String, String>> formatter) {
        private boolean supports(Object value) {
            return type.isInstance(value);
        }

        @SuppressWarnings("unchecked")
        private void apply(Object value, Map<String, String> target) {
            formatter.accept((T) value, target);
        }
    }

    private static final List<Registration<?>> REGISTRATIONS = List.of(
        new Registration<>(PermissionSyncPayload.class, (payload, data) -> put(data, "permissions", payload.permissions())),
        new Registration<>(PackListRequestPayload.class, (payload, data) -> {
        }),
        new Registration<>(PackCreatePayload.class, (payload, data) -> {
            put(data, "name", payload.name());
            put(data, "namespace", payload.namespace());
        }),
        new Registration<>(PackListSyncPayload.class, (payload, data) -> {
            put(data, "packCount", payload.packs().size());
            put(data, "packs", payload.packs().stream().map(pack -> pack.packId() + ":" + pack.name()).toList());
        }),
        new Registration<>(PackWorkspaceRequestPayload.class, (payload, data) -> {
            put(data, "packId", payload.packId());
            put(data, "registryId", payload.registryId());
        }),
        new Registration<>(PackWorkspaceSyncPayload.class, (payload, data) -> {
            put(data, "packId", payload.packId());
            put(data, "registryId", payload.registryId());
            put(data, "entryCount", payload.entries().size());
            if (!payload.entries().isEmpty())
                putSnapshot(data, "firstEntry.", payload.entries().getFirst());
        }),
        new Registration<>(WorkspaceMutationRequestPayload.class, (payload, data) -> {
            put(data, "actionId", payload.actionId());
            put(data, "packId", payload.packId());
            put(data, "registryId", payload.registryId());
            put(data, "targetId", payload.targetId());
            put(data, "action", payload.action());
        }),
        new Registration<>(WorkspaceSyncPayload.class, (payload, data) -> {
            put(data, "actionId", payload.actionId());
            put(data, "packId", payload.packId());
            put(data, "mutationResponse", payload.mutationResponse());
            put(data, "accepted", payload.accepted());
            put(data, "errorCode", payload.errorCode());
            if (payload.snapshot() != null)
                putSnapshot(data, "snapshot.", payload.snapshot());
        }));

    public static Details describe(CustomPacketPayload payload) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        if (payload == null)
            return new Details(Identifier.fromNamespaceAndPath("minecraft", "unknown"), "", "", Map.of());

        Identifier payloadId = payload.type().id();
        for (Registration<?> registration : REGISTRATIONS) {
            if (!registration.supports(payload))
                continue;

            registration.apply(payload, data);
            return new Details(payloadId, resolveTitle(payloadId), resolveDescription(payloadId), Map.copyOf(data));
        }

        put(data, "content", payload);
        return new Details(payloadId, resolveTitle(payloadId), resolveDescription(payloadId), Map.copyOf(data));
    }

    private static String resolveTitle(Identifier payloadId) {
        String key = translationKey(payloadId, "title");
        return I18n.exists(key) ? I18n.get(key) : payloadId.getPath();
    }

    private static String resolveDescription(Identifier payloadId) {
        String key = translationKey(payloadId, "description");
        return I18n.exists(key) ? I18n.get(key) : payloadId.toString();
    }

    private static String translationKey(Identifier payloadId, String suffix) {
        return "debug:payload." + payloadId.getNamespace() + "." + payloadId.getPath().replace('/', '.') + "." + suffix;
    }

    private static void putSnapshot(Map<String, String> data, String prefix, WorkspaceElementSnapshot snapshot) {
        put(data, prefix + "registryId", snapshot.registryId());
        put(data, prefix + "targetId", snapshot.targetId());
        put(data, prefix + "tags", snapshot.tags());
        put(data, prefix + "dataJson", snapshot.dataJson());
    }

    private static void put(Map<String, String> target, String key, Object value) {
        if (key == null || key.isBlank() || value == null)
            return;
        String text = String.valueOf(value);
        if (text.isBlank())
            return;
        target.put(key, text);
    }

    private ClientPayloadDebugFormatter() {}
}
