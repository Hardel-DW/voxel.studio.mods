package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class RegistryElementStore {

    private record SelectorKey(String registry, Identifier elementId) {}

    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final Map<SelectorKey, MutableSelectorStore<ElementEntry<?>>> elementStores = new HashMap<>();
    private final Map<String, List<Runnable>> registryListeners = new HashMap<>();

    public <T> void snapshotFromRegistry(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
        Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        registry.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                    key -> tagsByElement.computeIfAbsent(key.identifier(), ignored -> new HashSet<>()).add(tagId));
            }
        });

        Map<Identifier, ElementEntry<?>> entries = new LinkedHashMap<>();
        registry.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            ElementEntry<T> entry = new ElementEntry<>(id, holder.value(), Set.copyOf(tags), CustomFields.EMPTY);
            ElementEntry<T> enrichedEntry = entry.withCustom(customInitializer.apply(entry));
            entries.put(id, enrichedEntry);
            elementStore(name, id, enrichedEntry).setState(enrichedEntry);
        });

        current.put(name, new LinkedHashMap<>(entries));
        notifyRegistryListeners(name);
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> get(ResourceKey<Registry<T>> registry, Identifier id) {
        Map<Identifier, ElementEntry<?>> registryMap = current.get(registryName(registry));
        if (registryMap == null)
            return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    public <T> void put(ResourceKey<Registry<T>> registry, Identifier id, ElementEntry<T> entry) {
        String name = registryName(registry);
        Map<Identifier, ElementEntry<?>> registryMap = current.computeIfAbsent(name, ignored -> new LinkedHashMap<>());
        ElementEntry<?> previous = registryMap.put(id, entry);
        if (Objects.equals(previous, entry))
            return;
        elementStore(name, id, entry).setState(entry);
        notifyRegistryListeners(name);
    }

    public <T> void replaceAll(ResourceKey<Registry<T>> registry, Collection<ElementEntry<T>> entries) {
        String name = registryName(registry);
        Map<Identifier, ElementEntry<?>> previous = current.getOrDefault(name, Map.of());
        Map<Identifier, ElementEntry<?>> next = new LinkedHashMap<>();
        for (ElementEntry<T> entry : entries) {
            next.put(entry.id(), entry);
            elementStore(name, entry.id(), entry).setState(entry);
        }
        for (Identifier removedId : previous.keySet()) {
            if (!next.containsKey(removedId))
                elementStore(name, removedId, null).setState(null);
        }
        current.put(name, next);
        notifyRegistryListeners(name);
    }

    public <T> void subscribeRegistry(ResourceKey<Registry<T>> registry, Runnable listener) {
        List<Runnable> listeners = registryListeners.computeIfAbsent(registryName(registry), ignored -> new ArrayList<>());
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public <T> void unsubscribeRegistry(ResourceKey<Registry<T>> registry, Runnable listener) {
        List<Runnable> listeners = registryListeners.get(registryName(registry));
        if (listeners == null)
            return;
        listeners.remove(listener);
        if (listeners.isEmpty())
            registryListeners.remove(registryName(registry));
    }

    @SuppressWarnings("unchecked")
    public <T> List<ElementEntry<T>> allTypedElements(ResourceKey<Registry<T>> registry) {
        Map<Identifier, ElementEntry<?>> registryMap = current.get(registryName(registry));
        if (registryMap == null)
            return List.of();
        return registryMap.values().stream().map(entry -> (ElementEntry<T>) entry).toList();
    }

    @SuppressWarnings("unchecked")
    public <T, R> StoreSelection<ElementEntry<?>, R> selectValue(ResourceKey<Registry<T>> registry, Identifier id,
        Function<ElementEntry<T>, R> extractor) {
        String name = registryName(registry);
        ElementEntry<T> entry = get(registry, id);
        return elementStore(name, id, entry)
            .select(currentEntry -> currentEntry == null ? null : extractor.apply((ElementEntry<T>) currentEntry));
    }

    public void clearAll() {
        current.clear();
        elementStores.values().forEach(store -> store.setState(null));
    }

    private MutableSelectorStore<ElementEntry<?>> elementStore(String registry, Identifier id, ElementEntry<?> initial) {
        return elementStores.computeIfAbsent(new SelectorKey(registry, id),
            ignored -> new MutableSelectorStore<>(initial));
    }

    private void notifyRegistryListeners(String registry) {
        List<Runnable> listeners = registryListeners.get(registry);
        if (listeners != null)
            listeners.forEach(Runnable::run);
    }

    static String registryName(ResourceKey<?> key) {
        return key.identifier().toString();
    }
}
