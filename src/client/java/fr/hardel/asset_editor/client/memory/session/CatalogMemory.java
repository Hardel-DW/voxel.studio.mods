package fr.hardel.asset_editor.client.memory.session;

import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.client.memory.core.ReadableMemory;

import java.util.List;
import java.util.Map;

public final class CatalogMemory implements ReadableMemory<Map<String, List<?>>> {

    private final SimpleMemory<Map<String, List<?>>> memory = new SimpleMemory<>(Map.of());

    @Override
    public Map<String, List<?>> snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getCatalog(String registryId, Class<T> entryType) {
        List<?> raw = snapshot().get(registryId);
        if (raw == null || raw.isEmpty())
            return List.of();
        return (List<T>) raw;
    }

    public void updateCatalog(String registryId, List<?> entries) {
        List<?> safeEntries = entries == null ? List.of() : List.copyOf(entries);
        memory.update(state -> {
            var next = new java.util.HashMap<>(state);
            next.put(registryId, safeEntries);
            return Map.copyOf(next);
        });
    }

    public void clear() {
        memory.setSnapshot(Map.of());
    }
}
