package fr.hardel.asset_editor.client.debug;

import fr.hardel.asset_editor.client.javafx.lib.debug.DebugLogStore;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientDebugTelemetry {

    public static void networkInbound(CustomPacketPayload payload) {
        logNetworkPayload(payload, "S->C");
    }

    public static void networkOutbound(CustomPacketPayload payload) {
        logNetworkPayload(payload, "C->S");
    }

    public static void actionDispatched(String packId, Identifier registryId, Identifier targetId,
        EditorAction action, UUID actionId) {
        log(DebugLogStore.Level.INFO, DebugLogStore.Category.ACTION,
            I18n.get("debug:telemetry.dispatched_editor_action"),
            mapOf("packId", packId, "registryId", registryId, "targetId", targetId, "action", action, "actionId", actionId));
    }

    public static void sync(String message, Map<String, ?> data) {
        log(DebugLogStore.Level.INFO, DebugLogStore.Category.SYNC, message, data);
    }

    public static void lifecycle(String message, Map<String, ?> data) {
        log(DebugLogStore.Level.INFO, DebugLogStore.Category.LIFECYCLE, message, data);
    }

    public static void log(DebugLogStore.Level level, DebugLogStore.Category category, String message,
        Map<String, ?> data) {
        if (data == null) {
            DebugLogStore.log(level, category, message, Map.of());
            return;
        }

        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)
                continue;

            String text = String.valueOf(entry.getValue());
            if (text.isBlank())
                continue;

            normalized.put(entry.getKey(), text);
        }

        DebugLogStore.log(level, category, message, normalized);
    }

    private static void logNetworkPayload(CustomPacketPayload payload, String direction) {
        ClientPayloadDebugFormatter.Details details = ClientPayloadDebugFormatter.describe(payload);
        log(DebugLogStore.Level.INFO, DebugLogStore.Category.NETWORK,
            details.title(),
            merge(mapOf("direction", direction, "payloadId", details.id(), "payloadDescription", details.description()), details.data()));
    }

    private static Map<String, Object> mapOf(Object... entries) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            Object key = entries[i];
            if (!(key instanceof String textKey))
                continue;
            data.put(textKey, entries[i + 1]);
        }
        return data;
    }

    private static Map<String, Object> merge(Map<String, ?> base, Map<String, ?> extra) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        if (base != null)
            data.putAll(base);

        if (extra != null)
            data.putAll(extra);

        return data;
    }

    private ClientDebugTelemetry() {}
}
