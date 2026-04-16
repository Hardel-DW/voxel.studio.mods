package fr.hardel.asset_editor.data.concept;

import com.mojang.datafixers.util.Either;
import fr.hardel.asset_editor.data.StudioRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StudioRegistryResolver {

    public static List<Identifier> conceptIds(RegistryAccess registries) {
        Optional<Registry<StudioConceptDef>> registry = conceptRegistry(registries);
        return registry.map(studioConceptDefs -> studioConceptDefs.listElements()
            .map(reference -> reference.key().identifier())
            .toList()).orElseGet(List::of);

    }

    public static StudioConceptDef conceptDefinition(RegistryAccess registries, Identifier conceptId) {
        if (conceptId == null)
            return null;

        return conceptRegistry(registries)
            .flatMap(registry -> registry.get(ResourceKey.create(StudioRegistries.STUDIO_CONCEPT, conceptId)))
            .map(Holder.Reference::value)
            .orElse(null);
    }

    public static StudioConceptDef requireConceptDefinition(RegistryAccess registries, Identifier conceptId) {
        StudioConceptDef definition = conceptDefinition(registries, conceptId);
        if (definition == null)
            throw new IllegalArgumentException("Unknown studio concept '" + conceptId + "'");
        return definition;
    }

    public static Identifier conceptId(RegistryAccess registries, ResourceKey<? extends Registry<?>> registryKey) {
        if (registryKey == null)
            return null;

        return conceptIdForRegistry(registries, registryKey.identifier());
    }

    public static Identifier conceptIdForRegistry(RegistryAccess registries, Identifier registryId) {
        if (registryId == null)
            return null;

        return conceptIds(registries).stream()
            .filter(conceptId -> requireConceptDefinition(registries, conceptId).hasRegistry(registryId))
            .findFirst()
            .orElse(null);
    }

    public static Identifier defaultEditorTab(RegistryAccess registries, Identifier conceptId) {
        return requireConceptDefinition(registries, conceptId).defaultEditorTab();
    }

    public static List<Identifier> editorTabs(RegistryAccess registries, Identifier conceptId) {
        StudioConceptDef definition = requireConceptDefinition(registries, conceptId);
        Either<List<Identifier>, Identifier> tabs = definition.tabs();
        if (tabs.left().isPresent())
            return List.copyOf(tabs.left().get());

        Identifier tagId = tabs.right().orElse(null);
        if (tagId == null)
            return List.of();

        Optional<Registry<StudioEditorTabDef>> registry = editorTabRegistry(registries);
        if (registry.isEmpty())
            return List.of();

        Optional<? extends HolderSet<StudioEditorTabDef>> holderSet = registry.get().get(StudioTagKeys.tab(tagId));
        if (holderSet.isEmpty())
            return List.of();

        ArrayList<Identifier> resolved = new ArrayList<>();
        holderSet.get().stream()
            .map(holder -> holder.unwrapKey().orElse(null))
            .filter(Objects::nonNull)
            .map(ResourceKey::identifier)
            .forEach(resolved::add);
        return List.copyOf(resolved);
    }

    public static ResourceKey<? extends Registry<?>> registryKey(RegistryAccess registries, Identifier conceptId) {
        return ResourceKey.createRegistryKey(requireConceptDefinition(registries, conceptId).registry());
    }

    public static List<Identifier> registryIds(RegistryAccess registries, Identifier conceptId) {
        return requireConceptDefinition(registries, conceptId).registries();
    }

    public static String registryPath(RegistryAccess registries, Identifier conceptId) {
        return conceptId.getPath();
    }

    public static String titleKey(RegistryAccess registries, Identifier conceptId) {
        return "studio.concept." + registryPath(registries, conceptId);
    }

    public static String tabTitleKey(RegistryAccess registries, Identifier conceptId, Identifier tabId) {
        return "studio.concept." + registryPath(registries, conceptId) + ".tab." + tabId.getPath();
    }

    public static Identifier icon(RegistryAccess registries, Identifier conceptId) {
        return Identifier.withDefaultNamespace("textures/studio/concept/" + registryPath(registries, conceptId) + ".png");
    }

    public static Identifier elementIcon(Identifier registryId) {
        return Identifier.withDefaultNamespace("textures/studio/concept/" + registryId.getPath() + ".png");
    }

    private static Optional<Registry<StudioConceptDef>> conceptRegistry(RegistryAccess registries) {
        if (registries == null)
            return Optional.empty();

        return registries.lookup(StudioRegistries.STUDIO_CONCEPT);
    }

    private static Optional<Registry<StudioEditorTabDef>> editorTabRegistry(RegistryAccess registries) {
        if (registries == null)
            return Optional.empty();

        return registries.lookup(StudioRegistries.STUDIO_TAB);
    }

    private StudioRegistryResolver() {}
}
