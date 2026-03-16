package fr.hardel.asset_editor.client.javafx.lib.store;

import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import java.util.*;
import java.util.function.Function;

public final class RegistryElementStore {

    private record SelectorKey(String registry, Identifier elementId) {
    }

    private record RegistryRef<T>(ResourceKey<Registry<T>> key, Registry<T> registry) {
    }

    private final Map<String, Map<Identifier, ElementEntry<?>>> reference = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final Map<String, RegistryRef<?>> registryRefs = new HashMap<>();
    private final Map<SelectorKey, List<StoreSelector<?>>> selectors = new HashMap<>();
    private final Map<String, List<Runnable>> registryListeners = new HashMap<>();

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
                             ResourceManager referenceResources) {
        snapshot(registryKey, registry, referenceResources, entry -> CustomFields.EMPTY);
    }

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
                             ResourceManager referenceResources,
                             Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);
        registryRefs.put(name, new RegistryRef<>(registryKey, registry));

        Map<Identifier, Set<Identifier>> refTagsByElement = loadTags(registryKey, referenceResources, registry);

        Map<Identifier, Set<Identifier>> curTagsByElement = new HashMap<>();
        registry.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                        key -> curTagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });

        Map<Identifier, ElementEntry<?>> refEntries = new LinkedHashMap<>();
        Map<Identifier, ElementEntry<?>> curEntries = new LinkedHashMap<>();

        registry.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> refTags = refTagsByElement.getOrDefault(id, Set.of());
            Set<Identifier> curTags = curTagsByElement.getOrDefault(id, Set.of());

            ElementEntry<T> refEntry = new ElementEntry<>(id, holder.value(), Set.copyOf(refTags), CustomFields.EMPTY);
            refEntries.put(id, refEntry.withCustom(customInitializer.apply(refEntry)));

            ElementEntry<T> curEntry = new ElementEntry<>(id, holder.value(), Set.copyOf(curTags), CustomFields.EMPTY);
            curEntries.put(id, curEntry.withCustom(customInitializer.apply(curEntry)));
        });

        reference.put(name, Map.copyOf(refEntries));
        current.put(name, new LinkedHashMap<>(curEntries));
        notifyRegistryListeners(name);
    }

    public void reloadReference(ResourceManager referenceResources) {
        for (var ref : registryRefs.values()) {
            reloadReferenceFor(ref, referenceResources);
        }
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
        registryRefs.clear();
        selectors.values().forEach(list -> list.forEach(StoreSelector::dispose));
        selectors.clear();
    }

    private <T> void reloadReferenceFor(RegistryRef<T> ref, ResourceManager referenceResources) {
        String name = registryName(ref.key());
        var refMap = reference.get(name);
        if (refMap == null) return;

        Map<Identifier, Set<Identifier>> refTagsByElement = loadTags(ref.key(), referenceResources, ref.registry());

        Map<Identifier, ElementEntry<?>> updated = new LinkedHashMap<>();
        for (var entry : refMap.entrySet()) {
            Set<Identifier> tags = refTagsByElement.getOrDefault(entry.getKey(), Set.of());
            updated.put(entry.getKey(), entry.getValue().withTags(tags));
        }
        reference.put(name, Map.copyOf(updated));
    }

    @SuppressWarnings("unchecked")
    private <T> Map<Identifier, Set<Identifier>> loadTags(ResourceKey<Registry<T>> registryKey,
                                                           ResourceManager resourceManager,
                                                           Registry<T> registry) {
        var loader = new TagLoader<>(
                (TagLoader.ElementLookup<Holder<T>>) TagLoader.ElementLookup.fromFrozenRegistry(registry),
                Registries.tagsDirPath(registryKey));

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        loader.build(loader.load(resourceManager)).forEach((tagId, holders) -> {
            for (var holder : holders) {
                holder.unwrapKey().ifPresent(key ->
                        tagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });
        return tagsByElement;
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
