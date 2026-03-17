package fr.hardel.asset_editor.client.javafx.lib.store;

import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.*;
import java.util.function.Function;

public final class RegistryElementStore {

    private record SelectorKey(String registry, Identifier elementId) {
    }

    private final Map<String, Map<Identifier, ElementEntry<?>>> reference = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final Map<SelectorKey, List<StoreSelector<?>>> selectors = new HashMap<>();
    private final Map<String, List<Runnable>> registryListeners = new HashMap<>();

    public <T> void snapshotFromRegistry(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
                                          Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        registry.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                        key -> tagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });

        Map<Identifier, ElementEntry<?>> entries = new LinkedHashMap<>();
        registry.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            ElementEntry<T> entry = new ElementEntry<>(id, holder.value(), Set.copyOf(tags), CustomFields.EMPTY);
            entries.put(id, entry.withCustom(customInitializer.apply(entry)));
        });

        reference.put(name, Map.copyOf(entries));
        current.put(name, new LinkedHashMap<>(entries));
        notifyRegistryListeners(name);
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> get(ResourceKey<Registry<T>> registry, Identifier id) {
        var registryMap = current.get(registryName(registry));
        if (registryMap == null)
            return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    public <T> void put(ResourceKey<Registry<T>> registry, Identifier id, ElementEntry<T> entry) {
        String name = registryName(registry);
        var registryMap = current.computeIfAbsent(name, k -> new LinkedHashMap<>());
        var previous = registryMap.put(id, entry);
        if (Objects.equals(previous, entry))
            return;
        notifySelectors(name, id, entry);
        notifyRegistryListeners(name);
    }

    public <T> void subscribeRegistry(ResourceKey<Registry<T>> registry, Runnable listener) {
        var list = registryListeners.computeIfAbsent(registryName(registry), k -> new ArrayList<>());
        if (!list.contains(listener)) list.add(listener);
    }

    public <T> void unsubscribeRegistry(ResourceKey<Registry<T>> registry, Runnable listener) {
        var list = registryListeners.get(registryName(registry));
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) registryListeners.remove(registryName(registry));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> getReference(ResourceKey<Registry<T>> registry, Identifier id) {
        var registryMap = reference.get(registryName(registry));
        if (registryMap == null)
            return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    public <T> Collection<ElementEntry<?>> allElements(ResourceKey<Registry<T>> registry) {
        var registryMap = current.get(registryName(registry));
        return registryMap == null ? List.of() : registryMap.values();
    }

    @SuppressWarnings("unchecked")
    public <T> List<ElementEntry<T>> allTypedElements(ResourceKey<Registry<T>> registry) {
        var registryMap = current.get(registryName(registry));
        if (registryMap == null) return List.of();
        return registryMap.values().stream().map(e -> (ElementEntry<T>) e).toList();
    }

    public <T, R> StoreSelector<R> select(ResourceKey<Registry<T>> registry, Identifier id,
            Function<ElementEntry<T>, R> extractor) {
        String name = registryName(registry);
        ElementEntry<T> entry = get(registry, id);
        var selector = new StoreSelector<>(extractor, entry);
        selectors.computeIfAbsent(new SelectorKey(name, id), k -> new ArrayList<>()).add(selector);
        return selector;
    }

    public void disposeSelectors(List<StoreSelector<?>> toDispose) {
        for (var selector : toDispose) {
            selector.dispose();
        }
        for (var list : selectors.values()) {
            list.removeAll(toDispose);
        }
        selectors.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void clearAll() {
        reference.clear();
        current.clear();
        selectors.values().forEach(list -> list.forEach(StoreSelector::dispose));
        selectors.clear();
    }

    private void notifySelectors(String registry, Identifier id, ElementEntry<?> entry) {
        var list = selectors.get(new SelectorKey(registry, id));
        if (list == null) return;
        for (var selector : list) selector.recompute(entry);
    }

    private void notifyRegistryListeners(String registry) {
        var list = registryListeners.get(registry);
        if (list != null) list.forEach(Runnable::run);
    }

    static String registryName(ResourceKey<?> key) {
        return key.identifier().getPath();
    }
}
