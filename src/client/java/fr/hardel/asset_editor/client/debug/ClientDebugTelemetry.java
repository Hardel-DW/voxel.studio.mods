package fr.hardel.asset_editor.client.debug;

import fr.hardel.asset_editor.client.memory.ClientMemoryHolder;
import fr.hardel.asset_editor.client.memory.session.debug.DebugLogMemory;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientDebugTelemetry {

    public static void actionDispatched(String packId, Identifier registryId, Identifier targetId, EditorAction action, UUID actionId) {
        log(DebugLogMemory.Category.ACTION,
            I18n.get("debug:telemetry.dispatched_editor_action"),
            Map.of("packId", packId, "registryId", registryId.toString(),
                "targetId", targetId.toString(), "action", action.toString(), "actionId", actionId.toString()));
    }

    public static void optimisticFailed(Identifier registryId, Identifier targetId, EditorAction action, String reason) {
        logLevel(DebugLogMemory.Level.ERROR, DebugLogMemory.Category.ACTION,
            I18n.get("debug:telemetry.optimistic_failed"),
            Map.of("registryId", registryId.toString(), "targetId", targetId.toString(),
                "action", action.toString(), "reason", reason));
    }

    public static void actionRejected(UUID actionId, String errorCode, String detail) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("actionId", actionId.toString());
        data.put("errorCode", errorCode);
        if (detail != null && !detail.isBlank())
            data.put("detail", detail);

        logLevel(DebugLogMemory.Level.WARN, DebugLogMemory.Category.ACTION,
            I18n.get("debug:telemetry.action_rejected"),
            data);
    }

    public static void sync(String message, Map<String, ?> data) {
        log(DebugLogMemory.Category.SYNC, message, data);
    }

    public static void lifecycle(String message, Map<String, ?> data) {
        log(DebugLogMemory.Category.LIFECYCLE, message, data);
    }

    private static void log(DebugLogMemory.Category category, String message, Map<String, ?> data) {
        logLevel(DebugLogMemory.Level.INFO, category, message, data);
    }

    private static void logLevel(DebugLogMemory.Level level, DebugLogMemory.Category category, String message, Map<String, ?> data) {
        if (data == null) {
            ClientMemoryHolder.debug().logs().log(level, category, message);
            return;
        }

        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)
                continue;

            String text = String.valueOf(entry.getValue());
            if (!text.isBlank())
                normalized.put(entry.getKey(), text);
        }

        ClientMemoryHolder.debug().logs().log(level, category, message, normalized);
    }

    private ClientDebugTelemetry() {}
}
