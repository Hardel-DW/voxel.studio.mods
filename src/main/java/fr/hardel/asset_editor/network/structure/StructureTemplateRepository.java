package fr.hardel.asset_editor.network.structure;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side LRU cache for {@link StructureTemplateSnapshot}s, keyed by template id. Avoids rebuilding the full {@link StructureTemplateCatalog} on
 * every assembly request. Invalidated by {@code PackReloadEndEvent} (full clear) and by template writes (per-id).
 */
public final class StructureTemplateRepository {

    private static final StructureTemplateRepository INSTANCE = new StructureTemplateRepository();
    private static final int MAX_ENTRIES = 64;

    private final Map<Identifier, StructureTemplateSnapshot> cache = new LinkedHashMap<Identifier, StructureTemplateSnapshot>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Identifier, StructureTemplateSnapshot> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public static StructureTemplateRepository get() {
        return INSTANCE;
    }

    public synchronized Optional<StructureTemplateSnapshot> resolve(MinecraftServer server, Identifier templateId) {
        StructureTemplateSnapshot cached = cache.get(templateId);
        if (cached != null)
            return Optional.of(cached);

        StructureTemplateSnapshot fresh = StructureTemplateCatalog.build(server, List.of(templateId)).stream().findFirst().orElse(null);
        if (fresh != null)
            cache.put(templateId, fresh);

        return Optional.ofNullable(fresh);
    }

    public synchronized void invalidate(Identifier templateId) {
        cache.remove(templateId);
    }

    public synchronized void invalidateAll() {
        cache.clear();
    }

    private StructureTemplateRepository() {}
}
