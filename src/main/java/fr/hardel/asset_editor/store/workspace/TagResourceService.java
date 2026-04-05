package fr.hardel.asset_editor.store.workspace;

import fr.hardel.asset_editor.tag.TagSeed;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class TagResourceService {

    private final DiskWriter diskWriter = new DiskWriter();

    public boolean exists(Path packRoot, String registryPath, Identifier tagId, HolderLookup.Provider registries) {
        if (packRoot == null || registryPath == null || registryPath.isBlank() || tagId == null || registries == null)
            return false;

        if (Files.isRegularFile(tagPath(packRoot, registryPath, tagId)))
            return true;

        return lookupRegistry(registries, registryPath)
            .map(lookup -> runtimeTagExists(lookup, tagId))
            .orElse(false);
    }

    public boolean ensureExists(Path packRoot, String registryPath, Identifier tagId, TagSeed seed,
        HolderLookup.Provider registries) {
        if (seed == null || packRoot == null || registryPath == null || registryPath.isBlank() || tagId == null || registries == null)
            return false;

        if (exists(packRoot, registryPath, tagId, registries))
            return true;

        return diskWriter.writeTag(tagPath(packRoot, registryPath, tagId), seed.toTagFile());
    }

    private Path tagPath(Path packRoot, String registryPath, Identifier tagId) {
        return packRoot.resolve("data").resolve(tagId.getNamespace())
            .resolve("tags").resolve(registryPath).resolve(tagId.getPath() + ".json");
    }

    private Optional<HolderLookup.RegistryLookup<?>> lookupRegistry(HolderLookup.Provider registries,
        String registryPath) {
        return registries.listRegistries()
            .filter(lookup -> lookup.key().identifier().getPath().equals(registryPath))
            .findFirst();
    }

    @SuppressWarnings("unchecked")
    private boolean runtimeTagExists(HolderLookup.RegistryLookup<?> lookup, Identifier tagId) {
        try {
            HolderLookup.RegistryLookup<Object> typedLookup = (HolderLookup.RegistryLookup<Object>) lookup;
            ResourceKey<Registry<Object>> registryKey = (ResourceKey<Registry<Object>>) lookup.key();
            return typedLookup.get(TagKey.create(registryKey, tagId)).isPresent();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }
}
