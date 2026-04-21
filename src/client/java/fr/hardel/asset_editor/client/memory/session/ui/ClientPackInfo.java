package fr.hardel.asset_editor.client.memory.session.ui;

import fr.hardel.asset_editor.workspace.io.DataPackManager.PackEntry;

import java.util.List;

public record ClientPackInfo(String packId, String name, boolean writable, List<String> namespaces, byte[] icon) {

    public ClientPackInfo {
        packId = packId == null ? "" : packId;
        name = name == null ? "" : name;
        namespaces = List.copyOf(namespaces == null ? List.of() : namespaces);
        icon = icon == null ? new byte[0] : icon;
    }

    public static ClientPackInfo from(PackEntry entry) {
        return new ClientPackInfo(entry.packId(), entry.name(), entry.writable(), entry.namespaces(), entry.icon());
    }
}
