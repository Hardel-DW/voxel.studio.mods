package fr.hardel.asset_editor.store;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record CustomFields(Map<String, Object> values) {
    public static final CustomFields EMPTY = new CustomFields(Map.of());

    public CustomFields {
        if (!values.isEmpty()) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            values.forEach((key, value) -> {
                if (key != null && value != null)
                    normalized.put(key, normalizeValue(value));
            });
            values = normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
        } else {
            values = Map.of();
        }
    }

    public CustomFields with(String key, Object value) {
        if (value == null)
            return without(key);
        Map<String, Object> copy = new LinkedHashMap<>(values);
        copy.put(key, normalizeValue(value));
        return new CustomFields(copy);
    }

    public CustomFields without(String key) {
        if (!values.containsKey(key))
            return this;
        Map<String, Object> copy = new LinkedHashMap<>(values);
        copy.remove(key);
        return copy.isEmpty() ? EMPTY : new CustomFields(copy);
    }

    public String getString(String key, String fallback) {
        Object value = values.get(key);
        return value instanceof String string ? string : fallback;
    }

    public Set<String> getStringSet(String key) {
        Object value = values.get(key);
        if (!(value instanceof Set<?> set))
            return Set.of();
        if (set.stream().anyMatch(element -> !(element instanceof String)))
            return Set.of();
        return set.stream()
            .map(String.class::cast)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Set<?> set)
            return Set.copyOf(set);
        if (value instanceof List<?> list)
            return List.copyOf(list);
        if (value instanceof Map<?, ?> map)
            return Map.copyOf(map);
        return value;
    }
}
