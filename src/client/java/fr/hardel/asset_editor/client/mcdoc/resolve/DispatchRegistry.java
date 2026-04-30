package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Attributes;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.Index;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.TypeParam;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DispatchRegistry {

    public record Entry(
        List<Index> parallelIndices,
        List<TypeParam> typeParams,
        McdocType target,
        Attributes attributes
    ) {
        public Entry {
            parallelIndices = List.copyOf(parallelIndices);
            typeParams = List.copyOf(typeParams);
        }
    }

    private final Map<String, Map<String, Entry>> entries;

    private DispatchRegistry(Map<String, Map<String, Entry>> entries) {
        this.entries = entries;
    }

    public static Builder builder() { return new Builder(); }

    private static final String DEFAULT_NAMESPACE = "minecraft:";

    public Optional<Entry> resolve(String registry, String key) {
        Map<String, Entry> registryEntries = entries.get(registry);
        if (registryEntries == null) return Optional.empty();

        Entry exact = registryEntries.get(key);
        if (exact != null) return Optional.of(exact);

        Entry alt = registryEntries.get(toggleDefaultNamespace(key));
        if (alt != null) return Optional.of(alt);

        Entry fallback = registryEntries.get("%fallback");
        if (fallback != null) return Optional.of(fallback);

        return Optional.ofNullable(registryEntries.get("%unknown"));
    }

    private static String toggleDefaultNamespace(String key) {
        if (key.startsWith(DEFAULT_NAMESPACE)) return key.substring(DEFAULT_NAMESPACE.length());
        if (key.contains(":")) return key;
        return DEFAULT_NAMESPACE + key;
    }

    public Set<String> registries() {
        return entries.keySet();
    }

    public Map<String, Entry> entries(String registry) {
        return entries.getOrDefault(registry, Map.of());
    }

    public static final class Builder {

        private final Map<String, Map<String, Entry>> map = new HashMap<>();

        public void register(String registry, Entry entry) {
            Map<String, Entry> registryMap = map.computeIfAbsent(registry, ignored -> new LinkedHashMap<>());
            for (Index index : entry.parallelIndices()) {
                if (index instanceof McdocType.StaticIndex staticIndex) {
                    registryMap.put(staticIndex.value(), entry);
                }
            }
        }

        public void registerKey(String registry, String key, Entry entry) {
            map.computeIfAbsent(registry, ignored -> new LinkedHashMap<>()).put(key, entry);
        }

        public DispatchRegistry build() {
            Map<String, Map<String, Entry>> copy = new HashMap<>();
            map.forEach((reg, ents) -> copy.put(reg, Map.copyOf(ents)));
            return new DispatchRegistry(Map.copyOf(copy));
        }
    }
}
