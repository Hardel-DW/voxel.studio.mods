package fr.hardel.asset_editor.client.debug;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.client.memory.debug.DebugLogMemory;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientDebugTelemetry {

    public static void actionDispatched(String packId, Identifier registryId, Identifier targetId,
        EditorAction action, UUID actionId) {
        log(DebugLogMemory.Category.ACTION,
            I18n.get("debug:telemetry.dispatched_editor_action"),
            Map.of("packId", packId, "registryId", registryId.toString(),
                "targetId", targetId.toString(), "action", action.toString(), "actionId", actionId.toString()));
    }

    public static void sync(String message, Map<String, ?> data) {
        log(DebugLogMemory.Category.SYNC, message, data);
    }

    public static void lifecycle(String message, Map<String, ?> data) {
        log(DebugLogMemory.Category.LIFECYCLE, message, data);
    }

    private static void log(DebugLogMemory.Category category, String message,
                            Map<String, ?> data) {
        if (data == null) {
            AssetEditorClient.debugMemory().logs().log(DebugLogMemory.Level.INFO, category, message);
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

        AssetEditorClient.debugMemory().logs().log(DebugLogMemory.Level.INFO, category, message, normalized);
    }

    private ClientDebugTelemetry() {}
}
