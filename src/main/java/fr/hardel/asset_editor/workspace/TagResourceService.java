package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.io.DiskWriter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TagResourceService {

    private final DiskWriter diskWriter = new DiskWriter();

    public <T> boolean exists(Path packRoot, ResourceKey<Registry<T>> registryKey, Identifier tagId, HolderLookup.Provider registries) {
        if (packRoot == null || registryKey == null || tagId == null || registries == null)
            return false;

        if (Files.isRegularFile(tagPath(packRoot, registryKey, tagId)))
            return true;

        try {
            var lookup = registries.lookupOrThrow(registryKey);
            return lookup.get(TagKey.create(registryKey, tagId)).isPresent();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public <T> boolean ensureExists(Path packRoot, ResourceKey<Registry<T>> registryKey, Identifier tagId, TagSeed seed,
        HolderLookup.Provider registries) {
        if (seed == null || packRoot == null || registryKey == null || tagId == null || registries == null)
            return false;

        if (exists(packRoot, registryKey, tagId, registries))
            return true;

        return diskWriter.writeTag(tagPath(packRoot, registryKey, tagId), seed.toTagFile());
    }

    private Path tagPath(Path packRoot, ResourceKey<? extends Registry<?>> registryKey, Identifier tagId) {
        String registryPath = registryKey.identifier().getPath();
        return packRoot.resolve("data").resolve(tagId.getNamespace())
            .resolve("tags").resolve(registryPath).resolve(tagId.getPath() + ".json");
    }
}
