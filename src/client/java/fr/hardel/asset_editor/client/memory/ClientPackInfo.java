package fr.hardel.asset_editor.client.memory;

import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;

import java.util.List;

public record ClientPackInfo(String packId, String name, boolean writable, List<String> namespaces) {

    public ClientPackInfo {
        packId = packId == null ? "" : packId;
        name = name == null ? "" : name;
        namespaces = List.copyOf(namespaces == null ? List.of() : namespaces);
    }

    public static ClientPackInfo from(PackEntry entry) {
        return new ClientPackInfo(entry.packId(), entry.name(), entry.writable(), entry.namespaces());
    }
}
